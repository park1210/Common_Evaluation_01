package com.example.photoviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // =========================================================
    // UI
    // =========================================================

    private TextView textView;

    private RecyclerView recyclerView;

    private EditText editSearch;

    private Button btnSearch;
    private Button btnSort;
    private Button btnReset;
    private Button btnLoad;
    private Button btnUpload;


    // =========================================================
    // Server
    // =========================================================

    private static final String SITE_URL =
            "https://jaewoo1210.pythonanywhere.com";


    // =========================================================
    // 검색 / 정렬 상태
    // =========================================================

    private String currentSearch =
            "";


    /*
     * 기본 정렬
     * -created_date = 최신순
     */
    private String currentOrdering =
            "-created_date";


    // =========================================================
    // 기능 2
    // RecyclerView 위치 기억
    // =========================================================

    private static final String SCROLL_PREF_NAME =
            "PhotoViewerScrollState";


    private static final String KEY_SCROLL_POSITION =
            "scroll_position";


    private static final String KEY_SCROLL_OFFSET =
            "scroll_offset";


    private SharedPreferences scrollPreferences;

    private LinearLayoutManager layoutManager;


    /*
     * true:
     * 검색/정렬 때문에 새로운 목록을 표시하는 경우
     * 이전 Scroll 위치를 복원하지 않는다.
     */
    private boolean skipScrollRestore =
            false;


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
                R.layout.activity_main
        );


        // -----------------------------------------------------
        // UI 연결
        // -----------------------------------------------------

        textView =
                findViewById(
                        R.id.textView
                );


        recyclerView =
                findViewById(
                        R.id.recyclerView
                );


        editSearch =
                findViewById(
                        R.id.edit_search
                );


        btnSearch =
                findViewById(
                        R.id.btn_search
                );


        btnSort =
                findViewById(
                        R.id.btn_sort
                );


        btnReset =
                findViewById(
                        R.id.btn_reset
                );


        btnLoad =
                findViewById(
                        R.id.btn_load
                );


        btnUpload =
                findViewById(
                        R.id.btn_upload
                );


        // -----------------------------------------------------
        // RecyclerView
        // -----------------------------------------------------

        layoutManager =
                new LinearLayoutManager(
                        this
                );


        recyclerView.setLayoutManager(
                layoutManager
        );


        // -----------------------------------------------------
        // 기능 2 SharedPreferences
        // -----------------------------------------------------

        scrollPreferences =
                getSharedPreferences(
                        SCROLL_PREF_NAME,
                        MODE_PRIVATE
                );


        // -----------------------------------------------------
        // 스크롤이 멈추면 위치 저장
        // -----------------------------------------------------

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrollStateChanged(
                            RecyclerView recyclerView,
                            int newState
                    ) {

                        super.onScrollStateChanged(
                                recyclerView,
                                newState
                        );


                        if (
                                newState
                                        == RecyclerView.SCROLL_STATE_IDLE
                        ) {

                            saveScrollPosition();
                        }
                    }
                }
        );


        // =====================================================
        // 기존 동기화 버튼
        // =====================================================

        btnLoad.setOnClickListener(
                v -> {

                    skipScrollRestore =
                            false;


                    downloadPosts();
                }
        );


        // =====================================================
        // 게시물 작성 버튼
        // =====================================================

        btnUpload.setOnClickListener(
                v -> {

                    saveScrollPosition();


                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    UploadActivity.class
                            );


                    startActivity(
                            intent
                    );
                }
        );


        // =====================================================
        // 기능 4 - 검색 버튼
        // =====================================================

        btnSearch.setOnClickListener(
                v -> executeSearch()
        );


        /*
         * 키보드의 검색 버튼을 눌러도 검색 실행
         */
        editSearch.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (
                            actionId
                                    == EditorInfo.IME_ACTION_SEARCH
                    ) {

                        executeSearch();

                        return true;
                    }


                    return false;
                }
        );


        // =====================================================
        // 기능 4 - 정렬
        // =====================================================

        btnSort.setOnClickListener(
                v -> showSortDialog()
        );


        // =====================================================
        // 기능 4 - 초기화
        // =====================================================

        btnReset.setOnClickListener(
                v -> resetSearch()
        );
    }


    // =========================================================
    // onResume
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();


        /*
         * UploadActivity에서 돌아온 경우
         * 기존 검색/정렬 상태는 그대로 유지하면서
         * 서버 데이터를 다시 가져옴
         */
        skipScrollRestore =
                false;


        downloadPosts();
    }


    // =========================================================
    // onPause
    // =========================================================

    @Override
    protected void onPause() {

        saveScrollPosition();


        super.onPause();
    }


    // =========================================================
    // 검색 실행
    // =========================================================

    private void executeSearch() {

        currentSearch =
                editSearch
                        .getText()
                        .toString()
                        .trim();


        /*
         * 새로운 검색 결과에서는
         * 이전 전체 목록 위치가 의미가 없으므로 삭제
         */
        clearSavedScrollPosition();


        skipScrollRestore =
                true;


        downloadPosts();
    }


    // =========================================================
    // 검색 조건 초기화
    // =========================================================

    private void resetSearch() {

        editSearch.setText(
                ""
        );


        currentSearch =
                "";


        currentOrdering =
                "-created_date";


        btnSort.setText(
                "최신순"
        );


        clearSavedScrollPosition();


        skipScrollRestore =
                true;


        downloadPosts();


        Toast.makeText(
                MainActivity.this,
                "검색 및 정렬 조건을 초기화했습니다.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // 정렬 Dialog
    // =========================================================

    private void showSortDialog() {

        String[] sortItems = {

                "최신순",
                "오래된순",
                "제목순",
                "제목 역순"
        };


        int checkedItem;


        switch (currentOrdering) {

            case "created_date":

                checkedItem = 1;
                break;


            case "title":

                checkedItem = 2;
                break;


            case "-title":

                checkedItem = 3;
                break;


            case "-created_date":

            default:

                checkedItem = 0;
                break;
        }


        new AlertDialog.Builder(
                this
        )

                .setTitle(
                        "정렬 방법 선택"
                )

                .setSingleChoiceItems(
                        sortItems,
                        checkedItem,
                        (dialog, which) -> {


                            switch (which) {

                                // ---------------------------------
                                // 최신순
                                // ---------------------------------

                                case 0:

                                    currentOrdering =
                                            "-created_date";


                                    btnSort.setText(
                                            "최신순"
                                    );

                                    break;


                                // ---------------------------------
                                // 오래된순
                                // ---------------------------------

                                case 1:

                                    currentOrdering =
                                            "created_date";


                                    btnSort.setText(
                                            "오래된순"
                                    );

                                    break;


                                // ---------------------------------
                                // 제목순
                                // ---------------------------------

                                case 2:

                                    currentOrdering =
                                            "title";


                                    btnSort.setText(
                                            "제목순"
                                    );

                                    break;


                                // ---------------------------------
                                // 제목 역순
                                // ---------------------------------

                                case 3:

                                    currentOrdering =
                                            "-title";


                                    btnSort.setText(
                                            "제목 역순"
                                    );

                                    break;
                            }


                            dialog.dismiss();


                            clearSavedScrollPosition();


                            skipScrollRestore =
                                    true;


                            downloadPosts();
                        }
                )

                .setNegativeButton(
                        "취소",
                        null
                )

                .show();
    }


    // =========================================================
    // API URL 생성
    // =========================================================

    private String buildApiUrl()
            throws Exception {

        StringBuilder url =
                new StringBuilder();


        url.append(
                SITE_URL
        );


        url.append(
                "/api_root/Post/"
        );


        boolean hasParameter =
                false;


        // -----------------------------------------------------
        // search
        // -----------------------------------------------------

        if (
                currentSearch != null
                        && !currentSearch.isEmpty()
        ) {

            url.append(
                    "?search="
            );


            url.append(
                    URLEncoder.encode(
                            currentSearch,
                            StandardCharsets.UTF_8.toString()
                    )
            );


            hasParameter =
                    true;
        }


        // -----------------------------------------------------
        // ordering
        // -----------------------------------------------------

        if (
                currentOrdering != null
                        && !currentOrdering.isEmpty()
        ) {

            if (hasParameter) {

                url.append(
                        "&"
                );

            } else {

                url.append(
                        "?"
                );
            }


            url.append(
                    "ordering="
            );


            url.append(
                    URLEncoder.encode(
                            currentOrdering,
                            StandardCharsets.UTF_8.toString()
                    )
            );
        }


        return url.toString();
    }


    // =========================================================
    // 게시물 다운로드
    // =========================================================

    private void downloadPosts() {

        textView.setText(
                "서버에서 게시물을 불러오는 중..."
        );


        /*
         * 중복 클릭 방지
         */
        setLoadingState(
                true
        );


        new Thread(() -> {

            List<PostItem> postList =
                    new ArrayList<>();


            HttpURLConnection connection =
                    null;


            try {

                // -------------------------------------------------
                // API 요청
                // -------------------------------------------------

                String requestUrl =
                        buildApiUrl();


                URL apiUrl =
                        new URL(
                                requestUrl
                        );


                connection =
                        (HttpURLConnection)
                                apiUrl.openConnection();


                connection.setRequestMethod(
                        "GET"
                );


                connection.setConnectTimeout(
                        8000
                );


                connection.setReadTimeout(
                        10000
                );


                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );


                int responseCode =
                        connection.getResponseCode();


                if (
                        responseCode
                                != HttpURLConnection.HTTP_OK
                ) {

                    throw new Exception(
                            "서버 응답 오류: HTTP "
                                    + responseCode
                    );
                }


                // -------------------------------------------------
                // JSON 데이터 읽기
                // -------------------------------------------------

                InputStream inputStream =
                        connection.getInputStream();


                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        );


                StringBuilder result =
                        new StringBuilder();


                String line;


                while (
                        (line = reader.readLine())
                                != null
                ) {

                    result.append(
                            line
                    );
                }


                reader.close();

                inputStream.close();


                JSONArray jsonArray =
                        new JSONArray(
                                result.toString()
                        );


                // -------------------------------------------------
                // 각 게시물 처리
                // -------------------------------------------------

                for (
                        int i = 0;
                        i < jsonArray.length();
                        i++
                ) {

                    JSONObject post =
                            jsonArray.getJSONObject(
                                    i
                            );


                    String title =
                            post.optString(
                                    "title",
                                    "제목 없음"
                            );


                    String text =
                            post.optString(
                                    "text",
                                    ""
                            );


                    String publishedDate =
                            post.optString(
                                    "published_date",
                                    ""
                            );


                    String imageUrl =
                            post.optString(
                                    "image",
                                    ""
                            );


                    /*
                     * 현재 앱은 이미지 게시물을
                     * 보여주는 PhotoViewer이므로
                     * 이미지 URL이 없으면 제외
                     */
                    if (
                            imageUrl == null
                                    || imageUrl.trim().isEmpty()
                    ) {

                        continue;
                    }


                    /*
                     * 로컬 Django 호환 처리
                     *
                     * PythonAnywhere 주소에서는
                     * 아무 변화도 일어나지 않음
                     */
                    imageUrl =
                            imageUrl.replace(
                                    "127.0.0.1",
                                    "10.0.2.2"
                            );


                    Bitmap bitmap =
                            downloadBitmap(
                                    imageUrl
                            );


                    /*
                     * 특정 이미지 다운로드 실패가
                     * 전체 목록 실패로 이어지지 않게 함
                     */
                    if (bitmap == null) {

                        continue;
                    }


                    PostItem postItem =
                            new PostItem(
                                    title,
                                    text,
                                    publishedDate,
                                    bitmap
                            );


                    postList.add(
                            postItem
                    );
                }


                int totalResultCount =
                        jsonArray.length();


                // -------------------------------------------------
                // UI 갱신
                // -------------------------------------------------

                runOnUiThread(() -> {

                    setLoadingState(
                            false
                    );


                    // ---------------------------------------------
                    // 검색 결과 없음
                    // ---------------------------------------------

                    if (
                            totalResultCount == 0
                    ) {

                        textView.setText(
                                "검색 결과가 없습니다.\n"
                                        + "다른 검색어를 입력해 보세요."
                        );


                        recyclerView.setAdapter(
                                null
                        );


                        return;
                    }


                    // ---------------------------------------------
                    // 이미지 표시 가능한 게시물 없음
                    // ---------------------------------------------

                    if (
                            postList.isEmpty()
                    ) {

                        textView.setText(
                                "표시할 수 있는 게시물이 없습니다."
                        );


                        recyclerView.setAdapter(
                                null
                        );


                        return;
                    }


                    // ---------------------------------------------
                    // 정상 결과
                    // ---------------------------------------------

                    if (
                            currentSearch == null
                                    || currentSearch.isEmpty()
                    ) {

                        textView.setText(
                                "전체 게시물 "
                                        + totalResultCount
                                        + "개"
                        );

                    } else {

                        textView.setText(
                                "\""
                                        + currentSearch
                                        + "\" 검색 결과 "
                                        + totalResultCount
                                        + "개"
                        );
                    }


                    ImageAdapter adapter =
                            new ImageAdapter(
                                    postList
                            );


                    recyclerView.setAdapter(
                            adapter
                    );


                    // ---------------------------------------------
                    // 기능 2
                    // 일반 화면 복귀라면 이전 위치 복원
                    // ---------------------------------------------

                    if (!skipScrollRestore) {

                        recyclerView.post(
                                this::restoreScrollPosition
                        );
                    }


                    /*
                     * 한 번 처리한 뒤 기본값으로 돌림
                     */
                    skipScrollRestore =
                            false;
                });


            } catch (Exception e) {

                e.printStackTrace();


                String errorMessage =
                        e.getMessage() == null
                                ? "알 수 없는 오류가 발생했습니다."
                                : e.getMessage();


                runOnUiThread(() -> {

                    setLoadingState(
                            false
                    );


                    textView.setText(
                            "게시물을 불러오지 못했습니다."
                    );


                    Toast.makeText(
                            MainActivity.this,
                            errorMessage,
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
    // 개별 이미지 다운로드
    // =========================================================

    private Bitmap downloadBitmap(
            String imageUrl
    ) {

        HttpURLConnection imageConnection =
                null;


        InputStream imageStream =
                null;


        try {

            URL imageAddress =
                    new URL(
                            imageUrl
                    );


            imageConnection =
                    (HttpURLConnection)
                            imageAddress.openConnection();


            imageConnection.setConnectTimeout(
                    7000
            );


            imageConnection.setReadTimeout(
                    10000
            );


            imageConnection.setDoInput(
                    true
            );


            imageConnection.connect();


            int responseCode =
                    imageConnection.getResponseCode();


            if (
                    responseCode
                            != HttpURLConnection.HTTP_OK
            ) {

                return null;
            }


            imageStream =
                    imageConnection.getInputStream();


            return BitmapFactory.decodeStream(
                    imageStream
            );


        } catch (Exception e) {

            e.printStackTrace();


            return null;


        } finally {

            try {

                if (imageStream != null) {

                    imageStream.close();
                }

            } catch (Exception ignored) {
            }


            if (imageConnection != null) {

                imageConnection.disconnect();
            }
        }
    }


    // =========================================================
    // 로딩 중 UI 상태
    // =========================================================

    private void setLoadingState(
            boolean loading
    ) {

        btnLoad.setEnabled(
                !loading
        );


        btnSearch.setEnabled(
                !loading
        );


        btnSort.setEnabled(
                !loading
        );


        btnReset.setEnabled(
                !loading
        );
    }


    // =========================================================
    // 기능 2
    // Scroll 위치 저장
    // =========================================================

    private void saveScrollPosition() {

        if (
                layoutManager == null
                        || recyclerView == null
                        || scrollPreferences == null
        ) {

            return;
        }


        int position =
                layoutManager
                        .findFirstVisibleItemPosition();


        if (
                position
                        == RecyclerView.NO_POSITION
        ) {

            return;
        }


        View firstVisibleView =
                layoutManager.findViewByPosition(
                        position
                );


        int offset =
                0;


        if (firstVisibleView != null) {

            offset =
                    firstVisibleView.getTop()
                            - recyclerView.getPaddingTop();
        }


        scrollPreferences
                .edit()
                .putInt(
                        KEY_SCROLL_POSITION,
                        position
                )
                .putInt(
                        KEY_SCROLL_OFFSET,
                        offset
                )
                .apply();
    }


    // =========================================================
    // 기능 2
    // Scroll 위치 복원
    // =========================================================

    private void restoreScrollPosition() {

        if (
                layoutManager == null
                        || recyclerView == null
                        || scrollPreferences == null
        ) {

            return;
        }


        int savedPosition =
                scrollPreferences.getInt(
                        KEY_SCROLL_POSITION,
                        RecyclerView.NO_POSITION
                );


        int savedOffset =
                scrollPreferences.getInt(
                        KEY_SCROLL_OFFSET,
                        0
                );


        if (
                savedPosition
                        == RecyclerView.NO_POSITION
        ) {

            return;
        }


        RecyclerView.Adapter<?> adapter =
                recyclerView.getAdapter();


        if (adapter == null) {

            return;
        }


        int itemCount =
                adapter.getItemCount();


        if (itemCount <= 0) {

            return;
        }


        /*
         * 게시물 개수가 줄었을 경우
         * 유효 범위로 보정
         */
        if (
                savedPosition
                        >= itemCount
        ) {

            savedPosition =
                    itemCount - 1;
        }


        layoutManager.scrollToPositionWithOffset(
                savedPosition,
                savedOffset
        );


        Toast.makeText(
                this,
                "이전에 보던 위치에서 이어봅니다.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // 기능 2
    // 기존 Scroll 위치 제거
    // =========================================================

    private void clearSavedScrollPosition() {

        if (
                scrollPreferences == null
        ) {

            return;
        }


        scrollPreferences
                .edit()
                .remove(
                        KEY_SCROLL_POSITION
                )
                .remove(
                        KEY_SCROLL_OFFSET
                )
                .apply();
    }
}