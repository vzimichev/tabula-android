package technology.tabula;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.writers.CSVWriter;

@RunWith(AndroidJUnit4.class)
public class TabulaAndroidSmokeTest {

    @Test
    public void extractsTableFromUpstreamAnimalSoundsPdf() throws Exception {
        ExtractionSummary summary = extractCsvFromAsset("technology/tabula/AnimalSounds.pdf");
        assertCsvContains(summary.csv, "Cat", "Meow", "Fox");
    }

    @Test
    public void extractsTableFromUpstreamSchoolsPdf() throws Exception {
        ExtractionSummary summary = extractCsvFromAsset("technology/tabula/schools.pdf");
        assertTrue("Expected at least one extracted table from schools.pdf", summary.tableCount > 0);
    }

    @Test
    public void extractsTableFromUpstreamSpanningCellsPdf() throws Exception {
        ExtractionSummary summary = extractCsvFromAsset("technology/tabula/spanning_cells.pdf");
        assertTrue("Expected at least one extracted table from spanning_cells.pdf", summary.tableCount > 0);
    }

    @Test
    public void extractsMeaningfulContentFromLocalRaspBasisPdf() throws Exception {
        ExtractionSummary summary = extractCsvFromAsset("technology/tabula/rasp-basis.pdf");
        assertCsvContains(summary.csv, "გადახდის თარიღი", "4/14/2026", "70,440.91");
    }

    @Test
    public void extractsAtLeastOneTableFromLocalRaspBogPdf() throws Exception {
        ExtractionSummary summary = extractCsvFromAsset("technology/tabula/rasp-bog.pdf");
        assertTrue("Expected at least one extracted table from rasp-bog.pdf", summary.tableCount > 0);
    }

    @Test
    public void extractsAtLeastOneTableFromLocalRaspBogOnePdf() throws Exception {
        ExtractionSummary summary = extractCsvFromAsset("technology/tabula/rasp-bog-1.pdf");
        assertTrue("Expected at least one extracted table from rasp-bog-1.pdf", summary.tableCount > 0);
    }

    private ExtractionSummary extractCsvFromAsset(String assetPath) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PDFBoxResourceLoader.init(context);

        try (InputStream pdfStream = context.getAssets().open(assetPath);
             PDDocument document = PDDocument.load(pdfStream);
             ObjectExtractor extractor = new ObjectExtractor(document)) {
            SpreadsheetExtractionAlgorithm spreadsheet = new SpreadsheetExtractionAlgorithm();
            BasicExtractionAlgorithm basic = new BasicExtractionAlgorithm();
            CSVWriter csvWriter = new CSVWriter();
            StringBuilder out = new StringBuilder();
            int tableCount = 0;

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                Page page = extractor.extract(pageNumber);
                boolean useSpreadsheet = spreadsheet.isTabular(page);
                List<Table> tables = useSpreadsheet ? spreadsheet.extract(page) : basic.extract(page);
                if (tables.isEmpty()) {
                    continue;
                }
                tableCount += tables.size();
                csvWriter.write(out, tables);
                out.append('\n');
            }

            assertFalse("Expected at least one extracted table from " + assetPath, tableCount == 0);
            return new ExtractionSummary(tableCount, normalize(out.toString()));
        }
    }

    private void assertCsvContains(String actualCsv, String... expectedFragments) {
        for (String fragment : expectedFragments) {
            assertTrue("Expected extracted CSV to contain: " + fragment + "\nActual CSV:\n" + actualCsv,
                    actualCsv.contains(fragment));
        }
        assertTrue("Expected multiple rows of extracted content", actualCsv.split("\n").length >= 3);
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").trim();
    }

    private static final class ExtractionSummary {
        final int tableCount;
        final String csv;

        ExtractionSummary(int tableCount, String csv) {
            this.tableCount = tableCount;
            this.csv = csv;
        }
    }
}
