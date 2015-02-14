package org.maplebots.aap.erolls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.util.StringUtils;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class ECPDFExtractor {
	private Pattern pollingBoothName = Pattern.compile("^(\\d+)\\s\\1\\s(.*)");
	private Pattern pbPattern = Pattern.compile("^(\\d+)\\s\\1\\s(.*)(1\\..*)(All Voters)");
	private Pattern endOfPage = Pattern.compile("^Page Number : .*");
	private Pattern pincode = Pattern.compile(".*([0-9][0-9][0-9][0-9][0-9][0-9]).*");

	public void extractPollingBooths(String fileName, String outputFile) throws NumberFormatException, IOException {
		System.out.println("Extracting from " + fileName);
		LineNumberReader fileReader = new LineNumberReader(new FileReader(extract(fileName)));
		String line;
		StringBuilder pb = new StringBuilder();
		List<String> pollingBooths = new ArrayList<String>();

		boolean stopAdding = true;
		while ((line = fileReader.readLine()) != null) {
			Matcher pollingBoothNameMatcher = pollingBoothName.matcher(line);
			Matcher endOfPageMatcher = endOfPage.matcher(line);
			if (endOfPageMatcher.matches() || line.startsWith("Date")) {
				stopAdding = true;
			}
			if (pollingBoothNameMatcher.matches()) {
				stopAdding = false;
				// Output the previous polling booth if there
				if (pb.length() > 0) {
					pollingBooths.add(pb.toString());
					pb = new StringBuilder();
				}

				pb.append(line);
			} else if (!stopAdding) {
				pb.append(line);
			}
		}
		if (pb.length() > 0) {
			pollingBooths.add(pb.toString());
		}

		PrintWriter out = new PrintWriter(new FileOutputStream(outputFile));
		int errorCount = 0;
		for (String pbString : pollingBooths) {
			Matcher pbMatcher = pbPattern.matcher(pbString);
			if (pbMatcher.matches()) {
				PollingBooth pollingBooth = new PollingBooth();
				pollingBooth.setBoothNumber(pbMatcher.group(1));
				pollingBooth.setBoothName(pbMatcher.group(2));
				pollingBooth.setPollingAreas(pbMatcher.group(3));
				pollingBooth.setWhatVoters(pbMatcher.group(4));
				pollingBooth.setWhatVoters(pbMatcher.group(4));
				Matcher pincodeMatcher = pincode.matcher(pollingBooth.getBoothName());
				if (pincodeMatcher.matches()) {
					pollingBooth.setPincode(pincodeMatcher.group(1));
				}
				out.println(pollingBooth.toString());
			} else {
				errorCount++;
				System.out.println("ERROR>>>" + pbString);
			}
		}
		out.flush();
		out.close();
		fileReader.close();

		System.out.println("Extracted " + (pollingBooths.size() - errorCount) + " polling booths, with " + errorCount
				+ " errors to file " + outputFile);
	}

	private String extract(String fileName) {
		PdfReader reader = null;
		PrintWriter out = null;
		String outputFileName = fileName + ".tmp";
		try {
			reader = new PdfReader(fileName);
			// System.out.println(new String(reader.getMetadata()));
			PdfReaderContentParser parser = new PdfReaderContentParser(reader);
			out = new PrintWriter(new FileOutputStream(outputFileName));
			TextExtractionStrategy strategy;
			for (int i = 1; i <= reader.getNumberOfPages(); i++) {
				strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
				String pageText = strategy.getResultantText();
				out.println(pageText);
			}
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
		return outputFileName;
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		Options options = new Options();
		options.addOption("f", "inputFile", true, "Enter the input file");
		options.addOption("o", "outputFile", false, "Enter the output file");
		CommandLineParser parser = new BasicParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("f")) {
				String inputFile = line.getOptionValue("f");
				if (!new File(inputFile).exists()) {
					System.out.println(inputFile + " doesn't exist");
					return;
				}

				String outputFile = line.getOptionValue("o");
				if (!StringUtils.hasText(outputFile)) {
					outputFile = inputFile + ".psv";
				}
				new ECPDFExtractor().extractPollingBooths(inputFile, outputFile);
			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(ECPDFExtractor.class.getSimpleName(), options);
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}
	}
}

class PollingBooth {
	private String boothNumber;
	private String boothName;
	private String pincode;
	private String whatVoters;
	private String pollingAreas;

	public void setPollingAreas(String pollingAreas) {
		this.pollingAreas = pollingAreas;
	}

	public String getBoothNumber() {
		return boothNumber;
	}

	public void setBoothNumber(String boothNumber) {
		this.boothNumber = boothNumber;
	}

	public String getBoothName() {
		return boothName;
	}

	public void setBoothName(String boothName) {
		this.boothName = boothName;
	}

	public String getPollingAreas() {
		return pollingAreas.toString();
	}

	public String getPincode() {
		return pincode;
	}

	public void setPincode(String pincode) {
		this.pincode = pincode;
	}

	public String getWhatVoters() {
		return whatVoters;
	}

	public void setWhatVoters(String whatVoters) {
		this.whatVoters = whatVoters;
	}

	@Override
	public String toString() {
		return getBoothNumber() + "|" + getBoothName() + "|" + getPincode() + "|" + getWhatVoters() + "|"
				+ getPollingAreas();
	}
}