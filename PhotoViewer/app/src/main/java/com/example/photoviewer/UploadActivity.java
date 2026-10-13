package com.example.photoviewer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class UploadActivity extends AppCompatActivity {

    private EditText editTitle;
    private EditText editContent;

    private Button btnSelectImage;
    private Button btnUpload;

    private ImageView imagePreview;

    private TextView textSelectedImage;

    private ProgressBar progressBarUpload;

    private Uri selectedImageUri = null;

    /*
     * Android Emulator에서
     * Windows PC의 localhost 접근 주소
     */
    private static final String SITE_URL =
            "http://10.0.2.2:8000";

    /*
     * 현재 Django DB의 사용자 ID
     * superuser가 id=1인 경우 그대로 사용
     */
    private static final int AUTHOR_ID = 1;


    /*
     * Android 파일 선택기
     *
     * 별도의 READ_EXTERNAL_STORAGE 권한 없이
     * 이미지 파일을 선택할 수 있다.
     */
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            selectedImageUri = uri;

                            imagePreview.setImageURI(uri);

                            String fileName =
                                    getFileName(uri);

                            textSelectedImage.setText(
                                    "선택된 이미지: " + fileName
                            );
                        }
                    }
            );


    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_upload
        );


        editTitle =
                findViewById(
                        R.id.editTitle
                );

        editContent =
                findViewById(
                        R.id.editContent
                );

        btnSelectImage =
                findViewById(
                        R.id.btnSelectImage
                );

        btnUpload =
                findViewById(
                        R.id.btnUpload
                );

        imagePreview =
                findViewById(
                        R.id.imagePreview
                );

        textSelectedImage =
                findViewById(
                        R.id.textSelectedImage
                );

        progressBarUpload =
                findViewById(
                        R.id.progressBarUpload
                );


        /*
         * 이미지 선택 버튼
         */
        btnSelectImage.setOnClickListener(
                v -> imagePicker.launch(
                        "image/*"
                )
        );


        /*
         * 업로드 버튼
         */
        btnUpload.setOnClickListener(
                v -> validateAndUpload()
        );
    }


    /*
     * ==========================================
     * 입력값 검사
     * ==========================================
     */

    private void validateAndUpload() {

        String title =
                editTitle.getText()
                        .toString()
                        .trim();

        String content =
                editContent.getText()
                        .toString()
                        .trim();


        if (title.isEmpty()) {

            editTitle.setError(
                    "제목을 입력하세요."
            );

            editTitle.requestFocus();

            return;
        }


        if (content.isEmpty()) {

            editContent.setError(
                    "내용을 입력하세요."
            );

            editContent.requestFocus();

            return;
        }


        if (selectedImageUri == null) {

            Toast.makeText(
                    this,
                    "이미지를 선택하세요.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        uploadPost(
                title,
                content,
                selectedImageUri
        );
    }


    /*
     * ==========================================
     * Django REST API Multipart POST
     * ==========================================
     */

    private void uploadPost(
            String title,
            String content,
            Uri imageUri
    ) {

        setUploadingState(true);


        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String boundary =
                        "----PhotoViewerBoundary"
                                + System.currentTimeMillis();


                URL url = new URL(
                        SITE_URL
                                + "/api_root/Post/"
                );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "POST"
                );

                connection.setDoOutput(true);

                connection.setDoInput(true);

                connection.setUseCaches(false);


                connection.setConnectTimeout(
                        10000
                );

                connection.setReadTimeout(
                        15000
                );


                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary
                );


                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );


                /*
                 * 실제 요청 데이터 전송
                 */
                try (
                        DataOutputStream output =
                                new DataOutputStream(
                                        connection.getOutputStream()
                                )
                ) {

                    /*
                     * author
                     */
                    writeTextPart(
                            output,
                            boundary,
                            "author",
                            String.valueOf(
                                    AUTHOR_ID
                            )
                    );


                    /*
                     * title
                     */
                    writeTextPart(
                            output,
                            boundary,
                            "title",
                            title
                    );


                    /*
                     * text
                     */
                    writeTextPart(
                            output,
                            boundary,
                            "text",
                            content
                    );


                    /*
                     * published_date
                     */
                    String publishedDate =
                            createCurrentDateTime();

                    writeTextPart(
                            output,
                            boundary,
                            "published_date",
                            publishedDate
                    );


                    /*
                     * image
                     */
                    writeImagePart(
                            output,
                            boundary,
                            imageUri
                    );


                    /*
                     * Multipart 종료
                     */
                    output.writeBytes(
                            "--"
                                    + boundary
                                    + "--\r\n"
                    );


                    output.flush();
                }


                /*
                 * 서버 응답 코드
                 */
                int responseCode =
                        connection.getResponseCode();


                String responseBody =
                        readResponse(
                                connection,
                                responseCode
                        );


                /*
                 * Django REST Framework
                 * POST 성공 = HTTP 201 Created
                 */
                if (
                        responseCode
                                == HttpURLConnection.HTTP_CREATED
                ) {

                    runOnUiThread(() -> {

                        setUploadingState(false);

                        Toast.makeText(
                                UploadActivity.this,
                                "게시물이 업로드되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();


                        /*
                         * MainActivity로 돌아가기
                         */
                        setResult(
                                RESULT_OK
                        );

                        finish();
                    });

                } else {

                    runOnUiThread(() -> {

                        setUploadingState(false);

                        Toast.makeText(
                                UploadActivity.this,
                                "업로드 실패\nHTTP "
                                        + responseCode
                                        + "\n"
                                        + responseBody,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }


            } catch (Exception e) {

                e.printStackTrace();


                String errorMessage =
                        e.getMessage() == null
                                ? e.toString()
                                : e.getMessage();


                runOnUiThread(() -> {

                    setUploadingState(false);

                    Toast.makeText(
                            UploadActivity.this,
                            "업로드 중 오류가 발생했습니다.\n"
                                    + errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }

        }).start();
    }


    /*
     * ==========================================
     * Multipart Text Field 작성
     * ==========================================
     */

    private void writeTextPart(
            DataOutputStream output,
            String boundary,
            String fieldName,
            String value
    ) throws Exception {

        output.writeBytes(
                "--"
                        + boundary
                        + "\r\n"
        );


        output.writeBytes(
                "Content-Disposition: form-data; name=\""
                        + fieldName
                        + "\"\r\n"
        );


        output.writeBytes(
                "Content-Type: text/plain; charset=UTF-8\r\n"
        );


        output.writeBytes(
                "\r\n"
        );


        output.write(
                value.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        output.writeBytes(
                "\r\n"
        );
    }


    /*
     * ==========================================
     * Multipart Image Field 작성
     * ==========================================
     */

    private void writeImagePart(
            DataOutputStream output,
            String boundary,
            Uri imageUri
    ) throws Exception {

        ContentResolver resolver =
                getContentResolver();


        String fileName =
                getFileName(
                        imageUri
                );


        String mimeType =
                resolver.getType(
                        imageUri
                );


        if (mimeType == null) {

            mimeType =
                    "application/octet-stream";
        }


        output.writeBytes(
                "--"
                        + boundary
                        + "\r\n"
        );


        output.writeBytes(
                "Content-Disposition: form-data; "
                        + "name=\"image\"; "
                        + "filename=\""
                        + fileName
                        + "\"\r\n"
        );


        output.writeBytes(
                "Content-Type: "
                        + mimeType
                        + "\r\n"
        );


        output.writeBytes(
                "\r\n"
        );


        try (
                InputStream inputStream =
                        resolver.openInputStream(
                                imageUri
                        )
        ) {

            if (inputStream == null) {

                throw new Exception(
                        "선택한 이미지를 읽을 수 없습니다."
                );
            }


            byte[] buffer =
                    new byte[8192];


            int bytesRead;


            while (
                    (bytesRead =
                            inputStream.read(
                                    buffer
                            ))
                            != -1
            ) {

                output.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }


        output.writeBytes(
                "\r\n"
        );
    }


    /*
     * ==========================================
     * 선택한 파일 이름 얻기
     * ==========================================
     */

    private String getFileName(
            Uri uri
    ) {

        String result = null;


        if (
                "content".equals(
                        uri.getScheme()
                )
        ) {

            try (
                    Cursor cursor =
                            getContentResolver()
                                    .query(
                                            uri,
                                            null,
                                            null,
                                            null,
                                            null
                                    )
            ) {

                if (
                        cursor != null
                                && cursor.moveToFirst()
                ) {

                    int index =
                            cursor.getColumnIndex(
                                    OpenableColumns.DISPLAY_NAME
                            );


                    if (index >= 0) {

                        result =
                                cursor.getString(
                                        index
                                );
                    }
                }
            }
        }


        if (result == null) {

            String path =
                    uri.getPath();


            if (path != null) {

                int cut =
                        path.lastIndexOf(
                                '/'
                        );


                if (cut >= 0) {

                    result =
                            path.substring(
                                    cut + 1
                            );

                } else {

                    result = path;
                }
            }
        }


        if (
                result == null
                        || result.trim().isEmpty()
        ) {

            result =
                    "upload_image.jpg";
        }


        return result;
    }


    /*
     * ==========================================
     * 현재 날짜 ISO-8601
     *
     * 예:
     * 2026-10-13T15:30:00+09:00
     * ==========================================
     */

    private String createCurrentDateTime() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        Locale.getDefault()
                );


        return formatter.format(
                new Date()
        );
    }


    /*
     * ==========================================
     * 서버 응답 Body 읽기
     * ==========================================
     */

    private String readResponse(
            HttpURLConnection connection,
            int responseCode
    ) {

        StringBuilder result =
                new StringBuilder();


        try {

            InputStream inputStream;


            if (
                    responseCode >= 200
                            && responseCode < 300
            ) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }


            if (inputStream == null) {

                return "";
            }


            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            inputStream,
                                            StandardCharsets.UTF_8
                                    )
                            )
            ) {

                String line;


                while (
                        (line =
                                reader.readLine())
                                != null
                ) {

                    result.append(
                            line
                    );
                }
            }


        } catch (Exception e) {

            return e.toString();
        }


        return result.toString();
    }


    /*
     * ==========================================
     * 업로드 중 UI 제어
     * ==========================================
     */

    private void setUploadingState(
            boolean uploading
    ) {

        if (uploading) {

            progressBarUpload.setVisibility(
                    View.VISIBLE
            );

            btnUpload.setEnabled(
                    false
            );

            btnSelectImage.setEnabled(
                    false
            );

            editTitle.setEnabled(
                    false
            );

            editContent.setEnabled(
                    false
            );

        } else {

            progressBarUpload.setVisibility(
                    View.GONE
            );

            btnUpload.setEnabled(
                    true
            );

            btnSelectImage.setEnabled(
                    true
            );

            editTitle.setEnabled(
                    true
            );

            editContent.setEnabled(
                    true
            );
        }
    }
}