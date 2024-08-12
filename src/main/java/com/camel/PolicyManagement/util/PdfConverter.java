package com.camel.PolicyManagement.util;

import java.io.*;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;


@Component
public class PdfConverter {

    public static String htmlToXhtml(String html) {
        Document document = Jsoup.parse(html);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return document.html();
    }

    public static void xhtmlToPdf(String xhtml, String outputDirectory, String outFileName)
            throws IOException, DocumentException {
        // Create the output directory if it doesn't exist
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Combine the output directory path with the file name
        String fullOutputFileName = outputDirectory + File.separator + outFileName;

        File output = new File(fullOutputFileName);
        ITextRenderer iTextRenderer = new ITextRenderer();

        iTextRenderer.setDocumentFromString(xhtml);
        iTextRenderer.layout();
        OutputStream os = new FileOutputStream(output);
        iTextRenderer.createPDF(os);
        os.close();
    }

}
