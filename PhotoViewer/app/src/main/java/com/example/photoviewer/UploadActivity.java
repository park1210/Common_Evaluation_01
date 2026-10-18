package com.example.photoviewer;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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

    // =========================================================
    // UI
    // =========================================================

    private EditText editTitle;
    private EditText editContent;

    private Button btnSelectImage;
    private Button btnUpload;

    private ImageView imagePreview;

    private TextView textSelectedImage;

    private ProgressBar progressBarUpload;


    // =========================================================
    // 선택 이미지
    // =========================================================

    private Uri selectedImageUri = null;


    // =========================================================
    // Server
    // =========================================================

    private static final String SITE_URL =
            "https://jaewoo1210.pythonanywhere.com";

    private static final int AUTHOR_ID = 1;


    // =========================================================
    // Draft - SharedPreferences
    // =========================================================

    private static final String PREF_NAME =
            "PhotoViewerDraft";

    private static final String KEY_TITLE =
            "draft_title";

    private static final String KEY_CONTENT =
            "draft_content";

    private static final String KEY_IMAGE_URI =
            "draft_image_uri";

    private static final String KEY_HAS_DRAFT =
            "has_draft";


    private SharedPreferences draftPreferences;

    /*
     * Draft를 복원하면서 setText()를 호출할 때
     * TextWatcher가 다시 저장하는 것을 방지
     */
    private boolean restoringDraft = false;

    /*
     * Upload 성공 후 onPause()에서
     * Draft가 다시 저장되는 것을 방지
     */
    private boolean uploadCompleted = false;


    // =========================================================
    // 이미지 선택
    // =========================================================

    /*
     * GetContent 대신 OpenDocument 사용
     *
     * 이유:
     * 앱을 종료했다가 다시 실행해도
     * 선택한 이미지 URI 접근 권한을 유지하기 위함
     */
    private final ActivityResultLauncher<String[]> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {

                        if (uri == null) {
                            return;
                        }


                        selectedImageUri = uri;


                        /*
                         * 이미지 URI 접근 권한 유지
                         */
                        try {

                            getContentResolver()
                                    .takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    );

                        } catch (SecurityException e) {

                            e.printStackTrace();
                        }


                        /*
                         * Preview 표시
                         */
                        imagePreview.setImageURI(
                                uri
                        );


                        String fileName =
                                getFileName(
                                        uri
                                );


                        textSelectedImage.setText(
                                "선택된 이미지: "
                                        + fileName
                        );


                        /*
                         * 이미지 선택도 Draft에 저장
                         */
                        saveDraft();
                    }
            );


    // =========================================================
    // onCreate
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_upload
        );


        // -----------------------------------------------------
        // View 연결
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // SharedPreferences 초기화
        // -----------------------------------------------------

        draftPreferences =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );


        // -----------------------------------------------------
        // 제목 / 내용 자동 저장
        // -----------------------------------------------------

        TextWatcher draftWatcher =
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        /*
                         * Draft 복원 중에는 다시 저장하지 않음
                         */
                        if (!restoringDraft) {

                            saveDraft();
                        }
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                };


        editTitle.addTextChangedListener(
                draftWatcher
        );


        editContent.addTextChangedListener(
                draftWatcher
        );


        // -----------------------------------------------------
        // 이미지 선택 버튼
        // -----------------------------------------------------

        btnSelectImage.setOnClickListener(
                v -> imagePicker.launch(
                        new String[]{
                                "image/*"
                        }
                )
        );


        // -----------------------------------------------------
        // 게시 버튼
        // -----------------------------------------------------

        btnUpload.setOnClickListener(
                v -> validateAndUpload()
        );


        // -----------------------------------------------------
        // 기존 Draft가 존재하는지 확인
        // -----------------------------------------------------

        if (hasDraft()) {

            showDraftDialog();
        }
    }


    // =========================================================
    // 화면 이탈 시 Draft 저장
    // =========================================================

    @Override
    protected void onPause() {

        super.onPause();


        /*
         * 게시 완료 후에는 Draft를 다시 만들면 안 됨
         */
        if (!uploadCompleted) {

            saveDraft();
        }
    }


    // =========================================================
    // Draft 저장
    // =========================================================

    private void saveDraft() {

        if (draftPreferences == null) {
            return;
        }


        String title =
                editTitle.getText()
                        .toString();


        String content =
                editContent.getText()
                        .toString();


        String imageUriString = "";


        if (selectedImageUri != null) {

            imageUriString =
                    selectedImageUri.toString();
        }


        /*
         * 제목 / 내용 / 이미지 중 하나라도 존재하면
         * Draft 존재로 판단
         */
        boolean hasContent =
                !title.trim().isEmpty()
                        || !content.trim().isEmpty()
                        || selectedImageUri != null;


        SharedPreferences.Editor editor =
                draftPreferences.edit();


        editor.putString(
                KEY_TITLE,
                title
        );


        editor.putString(
                KEY_CONTENT,
                content
        );


        editor.putString(
                KEY_IMAGE_URI,
                imageUriString
        );


        editor.putBoolean(
                KEY_HAS_DRAFT,
                hasContent
        );


        editor.apply();
    }


    // =========================================================
    // Draft 존재 확인
    // =========================================================

    private boolean hasDraft() {

        if (draftPreferences == null) {
            return false;
        }


        return draftPreferences.getBoolean(
                KEY_HAS_DRAFT,
                false
        );
    }


    // =========================================================
    // Draft 복원
    // =========================================================

    private void restoreDraft() {

        restoringDraft = true;


        String title =
                draftPreferences.getString(
                        KEY_TITLE,
                        ""
                );


        String content =
                draftPreferences.getString(
                        KEY_CONTENT,
                        ""
                );


        String imageUriString =
                draftPreferences.getString(
                        KEY_IMAGE_URI,
                        ""
                );


        // -----------------------------------------------------
        // 제목 복원
        // -----------------------------------------------------

        editTitle.setText(
                title
        );


        // -----------------------------------------------------
        // 내용 복원
        // -----------------------------------------------------

        editContent.setText(
                content
        );


        /*
         * Cursor를 텍스트 마지막으로 이동
         */
        editTitle.setSelection(
                editTitle.length()
        );


        editContent.setSelection(
                editContent.length()
        );


        // -----------------------------------------------------
        // 이미지 복원
        // -----------------------------------------------------

        if (
                imageUriString != null
                        && !imageUriString.isEmpty()
        ) {

            try {

                selectedImageUri =
                        Uri.parse(
                                imageUriString
                        );


                imagePreview.setImageURI(
                        selectedImageUri
                );


                String fileName =
                        getFileName(
                                selectedImageUri
                        );


                textSelectedImage.setText(
                        "복원된 이미지: "
                                + fileName
                );


            } catch (Exception e) {

                e.printStackTrace();


                selectedImageUri = null;


                textSelectedImage.setText(
                        "이전 이미지를 불러올 수 없습니다."
                );


                /*
                 * 접근할 수 없는 URI는 제거
                 */
                draftPreferences
                        .edit()
                        .remove(
                                KEY_IMAGE_URI
                        )
                        .apply();
            }
        }


        restoringDraft = false;


        Toast.makeText(
                this,
                "작성 중이던 내용을 복원했습니다.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // Draft 삭제
    // =========================================================

    private void clearDraft() {

        if (draftPreferences != null) {

            draftPreferences
                    .edit()
                    .clear()
                    .apply();
        }


        selectedImageUri = null;
    }


    // =========================================================
    // Draft 선택 Dialog
    // =========================================================

    private void showDraftDialog() {

        new AlertDialog.Builder(
                this
        )

                .setTitle(
                        "작성 중인 게시물"
                )

                .setMessage(
                        "이전에 작성하던 게시물이 있습니다.\n\n"
                                + "이어서 작성하시겠습니까?"
                )

                .setPositiveButton(
                        "이어서 작성",
                        (dialog, which) -> {

                            restoreDraft();
                        }
                )

                .setNegativeButton(
                        "새로 작성",
                        (dialog, which) -> {

                            clearDraft();


                            restoringDraft = true;


                            editTitle.setText(
                                    ""
                            );


                            editContent.setText(
                                    ""
                            );


                            imagePreview.setImageDrawable(
                                    null
                            );


                            textSelectedImage.setText(
                                    "선택된 이미지가 없습니다."
                            );


                            restoringDraft = false;
                        }
                )

                /*
                 * Dialog 바깥을 눌러서 임의로 닫히지 않도록 설정
                 */
                .setCancelable(
                        false
                )

                .show();
    }


    // =========================================================
    // 입력값 검사
    // =========================================================

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


    // =========================================================
    // Django REST API Multipart POST
    // =========================================================

    private void uploadPost(
            String title,
            String content,
            Uri imageUri
    ) {

        setUploadingState(
                true
        );


        new Thread(() -> {

            HttpURLConnection connection = null;


            try {

                String boundary =
                        "----PhotoViewerBoundary"
                                + System.currentTimeMillis();


                URL url =
                        new URL(
                                SITE_URL
                                        + "/api_root/Post/"
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "POST"
                );


                connection.setDoOutput(
                        true
                );


                connection.setDoInput(
                        true
                );


                connection.setUseCaches(
                        false
                );


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


                // -------------------------------------------------
                // Multipart 전송
                // -------------------------------------------------

                try (
                        DataOutputStream output =
                                new DataOutputStream(
                                        connection.getOutputStream()
                                )
                ) {


                    // author
                    writeTextPart(
                            output,
                            boundary,
                            "author",
                            String.valueOf(
                                    AUTHOR_ID
                            )
                    );


                    // title
                    writeTextPart(
                            output,
                            boundary,
                            "title",
                            title
                    );


                    // text
                    writeTextPart(
                            output,
                            boundary,
                            "text",
                            content
                    );


                    // published_date
                    String publishedDate =
                            createCurrentDateTime();


                    writeTextPart(
                            output,
                            boundary,
                            "published_date",
                            publishedDate
                    );


                    // image
                    writeImagePart(
                            output,
                            boundary,
                            imageUri
                    );


                    // Multipart 종료
                    output.writeBytes(
                            "--"
                                    + boundary
                                    + "--\r\n"
                    );


                    output.flush();
                }


                // -------------------------------------------------
                // 서버 응답
                // -------------------------------------------------

                int responseCode =
                        connection.getResponseCode();


                String responseBody =
                        readResponse(
                                connection,
                                responseCode
                        );


                // -------------------------------------------------
                // HTTP 201
                // -------------------------------------------------

                if (
                        responseCode
                                == HttpURLConnection.HTTP_CREATED
                ) {

                    runOnUiThread(() -> {


                        setUploadingState(
                                false
                        );


                        /*
                         * 업로드가 완료되었기 때문에
                         * Draft 삭제
                         */
                        uploadCompleted = true;


                        clearDraft();


                        Toast.makeText(
                                UploadActivity.this,
                                "게시물이 업로드되었습니다.",
                                Toast.LENGTH_SHORT
                        ).show();


                        setResult(
                                RESULT_OK
                        );


                        finish();
                    });


                } else {

                    runOnUiThread(() -> {


                        setUploadingState(
                                false
                        );


                        /*
                         * 실패한 경우 Draft는 유지
                         */
                        saveDraft();


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


                    setUploadingState(
                            false
                    );


                    /*
                     * 오류가 나더라도 Draft 유지
                     */
                    saveDraft();


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


    // =========================================================
    // Multipart Text Field
    // =========================================================

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


    // =========================================================
    // Multipart Image Field
    // =========================================================

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


    // =========================================================
    // 선택 파일 이름 얻기
    // =========================================================

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

                    result =
                            path;
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


    // =========================================================
    // 현재 날짜 ISO-8601
    // =========================================================

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


    // =========================================================
    // Response Body
    // =========================================================

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


    // =========================================================
    // Upload UI 상태
    // =========================================================

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