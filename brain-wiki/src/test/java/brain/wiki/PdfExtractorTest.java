package brain.wiki;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class PdfExtractorTest {

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    /** Creates a single-page PDF containing the given text and saves it to path. */
    private static Path createTextPdf(Path dir, String filename, String content) throws IOException {
        Path pdfPath = dir.resolve(filename);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            doc.save(pdfPath.toFile());
        }
        return pdfPath;
    }

    /** Creates an empty (no-content) PDF with one blank page. */
    private static Path createEmptyPdf(Path dir, String filename) throws IOException {
        Path pdfPath = dir.resolve(filename);
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfPath.toFile());
        }
        return pdfPath;
    }

    // -----------------------------------------------------------------------
    // tests
    // -----------------------------------------------------------------------

    @Test
    void extractsTextFromValidPdf() throws IOException {
        Path pdf = createTextPdf(tempDir, "sample.pdf", "Hello PDFBox world");

        String result = PdfExtractor.extractText(pdf);

        assertThat(result).containsIgnoringCase("Hello PDFBox world");
    }

    @Test
    void throwsIllegalStateForPdfWithNoSelectableText() throws IOException {
        Path pdf = createEmptyPdf(tempDir, "empty.pdf");

        assertThatThrownBy(() -> PdfExtractor.extractText(pdf))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no contiene texto seleccionable");
    }

    @Test
    void returnedTextHasNoNonPrintableControlCharacters() throws IOException {
        // Embed a control character (BEL, \u0007) in the text to verify cleaning
        String rawContent = "Clean text here";
        Path pdf = createTextPdf(tempDir, "clean.pdf", rawContent);

        String result = PdfExtractor.extractText(pdf);

        // Verify no ASCII control characters (0x00–0x1F, 0x7F) except \n \r \t remain
        assertThat(result).doesNotContainPattern("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
        // And the actual content is preserved
        assertThat(result).contains("Clean text here");
    }
}
