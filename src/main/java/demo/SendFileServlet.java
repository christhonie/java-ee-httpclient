package demo;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@WebServlet("/send-file")
public class SendFileServlet extends HttpServlet {

    private static final String TARGET_URL =
            "https://daa-dev.development.creditguarantee.co.za/api/submissions";

    private static final String FILE_NAME = "452852-D2-20250929105010198.xlsx";
    private static final String FILE_NAME_HEADER = "452852-D2-20250929105010198";
    private static final String MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Load the hardcoded file from WEB-INF
        ServletContext ctx = getServletContext();
        try (InputStream fileStream =
                     ctx.getResourceAsStream("/WEB-INF/" + FILE_NAME)) {

            if (fileStream == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain");
                resp.getWriter().println("Could not find file in /WEB-INF: " + FILE_NAME);
                return;
            }

            byte[] fileBytes = toByteArray(fileStream);

            // 2. Build HTTP client and POST request (HTTP/1.1 by default)
            try (CloseableHttpClient client = HttpClients.createDefault()) {

                HttpPost httpPost = new HttpPost(TARGET_URL);

                // Custom headers from your curl example
                httpPost.setHeader("fileName", FILE_NAME_HEADER);
                httpPost.setHeader("mimeType", MIME_TYPE);

                // 3. Build multipart/form-data entity
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                // Binary part (file)
                builder.addBinaryBody(
                        "file",
                        fileBytes,
                        ContentType.create(MIME_TYPE),
                        FILE_NAME
                );

                // Text parts (form fields)
                builder.addTextBody("policyHolderId", "9991234567",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("reportingPeriodId", "3101",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("documentId", "af2683d0-18d0-43da-878b-54d5ee8c4bff",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("accountingPackageId", "1600",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("policyNumber", "452852",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("userId", "CGEJA",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));

                HttpEntity multipart = builder.build();
                httpPost.setEntity(multipart);

                // 4. Execute the request
                try (CloseableHttpResponse upstreamResponse = client.execute(httpPost)) {
                    int statusCode = upstreamResponse.getStatusLine().getStatusCode();
                    String body = upstreamResponse.getEntity() != null
                            ? EntityUtils.toString(upstreamResponse.getEntity(), StandardCharsets.UTF_8)
                            : "";

                    // 5. Return upstream response to the browser for debugging
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().println("Request sent to: " + TARGET_URL);
                    resp.getWriter().println("Status from API: " + statusCode);
                    resp.getWriter().println("Response body:");
                    resp.getWriter().println(body);
                }
            }
        }
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int read;
        while ((read = in.read(tmp)) != -1) {
            buffer.write(tmp, 0, read);
        }
        return buffer.toByteArray();
    }
}
