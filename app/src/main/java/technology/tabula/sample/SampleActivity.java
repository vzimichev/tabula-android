package technology.tabula.sample;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.writers.CSVWriter;

public class SampleActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView statusText;
    private TextView outputText;

    private final ActivityResultLauncher<String[]> openPdfLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPdfSelected);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        PDFBoxResourceLoader.init(getApplicationContext());

        statusText = findViewById(R.id.statusText);
        outputText = findViewById(R.id.outputText);
        MaterialButton openButton = findViewById(R.id.openButton);
        MaterialButton sampleButton = findViewById(R.id.sampleButton);

        openButton.setOnClickListener(v -> openPdfLauncher.launch(new String[]{"application/pdf"}));
        sampleButton.setOnClickListener(v -> runExtraction(new InputStreamProvider() {
            @Override
            public InputStream open() throws IOException {
                return getAssets().open("technology/tabula/AnimalSounds.pdf");
            }

            @Override
            public String label() {
                return "Bundled AnimalSounds.pdf";
            }
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void onPdfSelected(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        runExtraction(new InputStreamProvider() {
            @Override
            public InputStream open() throws IOException {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new IOException("Unable to open selected PDF");
                }
                return inputStream;
            }

            @Override
            public String label() {
                return uri.toString();
            }
        });
    }

    private void runExtraction(InputStreamProvider provider) {
        statusText.setText("Extracting " + provider.label() + " ...");
        outputText.setText("");

        executor.execute(() -> {
            try (InputStream inputStream = provider.open();
                 PDDocument document = PDDocument.load(inputStream);
                 ObjectExtractor extractor = new ObjectExtractor(document)) {

                StringBuilder rendered = new StringBuilder();
                SpreadsheetExtractionAlgorithm spreadsheet = new SpreadsheetExtractionAlgorithm();
                BasicExtractionAlgorithm basic = new BasicExtractionAlgorithm();
                CSVWriter csvWriter = new CSVWriter();

                int tableCount = 0;
                for (int pageIndex = 1; pageIndex <= document.getNumberOfPages(); pageIndex++) {
                    Page page = extractor.extract(pageIndex);
                    List<Table> tables = spreadsheet.extract(page);
                    String mode = "lattice";
                    if (tables.isEmpty()) {
                        tables = basic.extract(page);
                        mode = "stream";
                    }
                    if (tables.isEmpty()) {
                        continue;
                    }

                    tableCount += tables.size();
                    rendered.append("Page ").append(pageIndex)
                            .append(" (").append(mode).append(", ")
                            .append(tables.size()).append(" tables)")
                            .append("\n\n");
                    csvWriter.write(rendered, tables);
                    rendered.append("\n\n");
                }

                final int finalTableCount = tableCount;
                final String finalOutput = rendered.length() == 0 ? "No tables detected." : rendered.toString().trim();
                runOnUiThread(() -> {
                    statusText.setText("Done. Extracted " + finalTableCount + " table(s) from " + provider.label());
                    outputText.setText(finalOutput);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    statusText.setText("Extraction failed.");
                    outputText.setText(exception.toString());
                });
            }
        });
    }

    private interface InputStreamProvider {
        InputStream open() throws IOException;

        String label();
    }
}
