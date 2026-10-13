package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private RecyclerView recyclerView;

    private final String siteUrl =
            "http://10.0.2.2:8000";


    @Override
    protected void onResume() {

        super.onResume();

        downloadImages();
    }

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_main
        );


        textView =
                findViewById(
                        R.id.textView
                );


        recyclerView =
                findViewById(
                        R.id.recyclerView
                );


        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );


        Button btnLoad =
                findViewById(
                        R.id.btn_load
                );


        Button btnUpload =
                findViewById(
                        R.id.btn_upload
                );


        btnLoad.setOnClickListener(
                v -> downloadImages()
        );


        btnUpload.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            UploadActivity.class
                    );

            startActivity(intent);
        });
    }


    private void downloadImages() {

        textView.setText(
                "서버에서 이미지를 불러오는 중..."
        );


        new Thread(() -> {

            List<Bitmap> bitmapList =
                    new ArrayList<>();


            try {

                URL apiUrl = new URL(
                        siteUrl
                                + "/api_root/Post/"
                );


                HttpURLConnection conn =
                        (HttpURLConnection)
                                apiUrl.openConnection();


                conn.setRequestMethod("GET");

                conn.setConnectTimeout(5000);

                conn.setReadTimeout(5000);


                int responseCode =
                        conn.getResponseCode();


                if (
                        responseCode
                                == HttpURLConnection.HTTP_OK
                ) {

                    InputStream inputStream =
                            conn.getInputStream();


                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            inputStream
                                    )
                            );


                    StringBuilder result =
                            new StringBuilder();


                    String line;


                    while (
                            (line = reader.readLine())
                                    != null
                    ) {

                        result.append(line);
                    }


                    reader.close();

                    inputStream.close();


                    JSONArray jsonArray =
                            new JSONArray(
                                    result.toString()
                            );


                    for (
                            int i = 0;
                            i < jsonArray.length();
                            i++
                    ) {

                        JSONObject post =
                                jsonArray
                                        .getJSONObject(i);


                        String imageUrl =
                                post.optString(
                                        "image",
                                        ""
                                );


                        if (!imageUrl.isEmpty()) {

                            /*
                             * Django가 image URL을
                             * 127.0.0.1로 반환할 경우
                             * Android Emulator에서는
                             * 10.0.2.2로 변경해야 한다.
                             */

                            imageUrl =
                                    imageUrl.replace(
                                            "127.0.0.1",
                                            "10.0.2.2"
                                    );


                            URL imageAddress =
                                    new URL(
                                            imageUrl
                                    );


                            HttpURLConnection imageConn =
                                    (HttpURLConnection)
                                            imageAddress
                                                    .openConnection();


                            imageConn.setConnectTimeout(
                                    5000
                            );


                            imageConn.setReadTimeout(
                                    5000
                            );


                            InputStream imageStream =
                                    imageConn
                                            .getInputStream();


                            Bitmap bitmap =
                                    BitmapFactory
                                            .decodeStream(
                                                    imageStream
                                            );


                            if (bitmap != null) {

                                bitmapList.add(
                                        bitmap
                                );
                            }


                            imageStream.close();

                            imageConn.disconnect();
                        }
                    }
                }


                conn.disconnect();


                runOnUiThread(() -> {

                    if (bitmapList.isEmpty()) {

                        textView.setText(
                                "불러올 이미지가 없습니다."
                        );

                    } else {

                        textView.setText(
                                "이미지 로드 성공! "
                                        + bitmapList.size()
                                        + "개"
                        );


                        ImageAdapter adapter =
                                new ImageAdapter(
                                        bitmapList
                                );


                        recyclerView.setAdapter(
                                adapter
                        );
                    }
                });


            } catch (Exception e) {

                e.printStackTrace();


                runOnUiThread(() -> {

                    textView.setText(
                            "서버 연결 실패"
                    );


                    Toast.makeText(
                            MainActivity.this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }
}