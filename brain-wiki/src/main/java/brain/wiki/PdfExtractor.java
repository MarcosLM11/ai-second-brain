package brain.wiki;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Extracts plain text from PDF files using Apache PDFBox 3.x.
 *
 * <p>Only PDFs with selectable (embedded) text are supported.
 * Scanned image-only PDFs will produce an {@link IllegalStateException}.
 */
public class PdfExtractor {

    private PdfExtractor() {}

    /**
     * Extract all text from a PDF file.
     *
     * @param pdfPath absolute path to the PDF file
     * @return cleaned text content (never blank)
     * @throws IOException              if the file cannot be read or is not a valid PDF
     * @throws IllegalStateException    if the PDF contains no selectable text
     */
    public static String extractText(Path pdfPath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException(
                        "Este PDF no contiene texto seleccionable: " + pdfPath.getFileName());
            }
            // Remove control characters except newline (\n), carriage return (\r), and tab (\t)
            return text.replaceAll("[\\p{Cntrl}&&[^\n\r\t]]", "");
        }
    }
}
