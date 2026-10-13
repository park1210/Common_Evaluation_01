import shutil
import tempfile

from io import BytesIO
from PIL import Image

from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.utils import timezone

from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post

def create_test_image(filename="test.jpg"):

    buffer = BytesIO()

    image = Image.new(
        mode="RGB",
        size=(100, 100),
        color=(255, 255, 255)
    )

    image.save(
        buffer,
        format="JPEG"
    )

    image.close()

    buffer.seek(0)

    image_bytes = buffer.read()

    buffer.close()

    return SimpleUploadedFile(
        name=filename,
        content=image_bytes,
        content_type="image/jpeg"
    )


class PhotoBlogAPITest(APITestCase):

    API_URL = "/api_root/Post/"

    def setUp(self):

        # 테스트 전용 media 폴더 생성
        self.temp_media = tempfile.mkdtemp()

        self.media_override = override_settings(
            MEDIA_ROOT=self.temp_media
        )

        self.media_override.enable()

        # 테스트 사용자 생성
        self.user = User.objects.create_user(
            username="testuser",
            password="testpassword"
        )

        # GET 테스트에서 사용할 기존 게시물 생성
        image = create_test_image("test_image.jpg")

        self.post = Post.objects.create(
            author=self.user,
            title="API GET Test",
            text="Automatic API GET test",
            published_date=timezone.now(),
            image=image
        )

    def tearDown(self):

        self.media_override.disable()

        shutil.rmtree(
            self.temp_media,
            ignore_errors=True
        )

    # -------------------------------------------------
    # 1. GET /api_root/Post/
    # -------------------------------------------------

    def test_get_post_list(self):

        response = self.client.get(
            self.API_URL
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )

        # JSON Array인지 확인
        self.assertIsInstance(
            response.data,
            list
        )

        self.assertGreaterEqual(
            len(response.data),
            1
        )

        first_post = response.data[0]

        # 필요한 필드 검사
        required_fields = [
            "author",
            "title",
            "text",
            "created_date",
            "published_date",
            "image"
        ]

        for field in required_fields:

            self.assertIn(
                field,
                first_post
            )

    # -------------------------------------------------
    # 2. 이미지 URL 존재 여부
    # -------------------------------------------------

    def test_get_post_contains_image_url(self):

        response = self.client.get(
            self.API_URL
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )

        image_url = response.data[0]["image"]

        self.assertIsNotNone(
            image_url
        )

        self.assertIn(
            "/media/",
            image_url
        )

    # -------------------------------------------------
    # 3. POST /api_root/Post/
    # -------------------------------------------------

    def test_create_post_with_image(self):

        image = create_test_image("upload_test.jpg")

        data = {
            "author": self.user.id,
            "title": "API POST Test",
            "text": "Automatic API POST test",
            "published_date": timezone.now(),
            "image": image
        }

        response = self.client.post(
            self.API_URL,
            data,
            format="multipart"
        )
        print("POST response:", response.status_code)
        print("POST data:", response.data)

        self.assertEqual(
            response.status_code,
            status.HTTP_201_CREATED
        )

        self.assertTrue(
            Post.objects.filter(
                title="API POST Test"
            ).exists()
        )

        self.assertIn(
            "image",
            response.data
        )

    # -------------------------------------------------
    # 4. 잘못된 POST 검사
    # -------------------------------------------------

    def test_create_post_without_required_fields(self):

        response = self.client.post(
            self.API_URL,
            {
                "title": "Invalid Post"
            },
            format="multipart"
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_400_BAD_REQUEST
        )

    # -------------------------------------------------
    # 5. 일반 Blog 페이지
    # -------------------------------------------------

    def test_blog_list_page(self):

        response = self.client.get("/")

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )

        self.assertContains(
            response,
            "API GET Test"
        )

    # -------------------------------------------------
    # 6. 상세 페이지
    # -------------------------------------------------

    def test_blog_detail_page(self):

        response = self.client.get(
            f"/post/{self.post.id}/"
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )

        self.assertContains(
            response,
            "API GET Test"
        )

    # -------------------------------------------------
    # 7. 새 게시물 작성 화면
    # -------------------------------------------------

    def test_post_new_page(self):

        response = self.client.get(
            "/post/new/"
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )

    # -------------------------------------------------
    # 8. JS Test 페이지
    # -------------------------------------------------

    def test_js_test_page(self):

        response = self.client.get(
            "/js_test/"
        )

        self.assertEqual(
            response.status_code,
            status.HTTP_200_OK
        )


    def test_generated_image_is_valid(self):

        uploaded_file = create_test_image(
            "validation_test.jpg"
        )

        image_stream = BytesIO(
            uploaded_file.read()
        )

        image = Image.open(
            image_stream
        )

        image.verify()

        self.assertEqual(
            image.format,
            "JPEG"
        )

    def test_post_publish(self):

        self.post.published_date = None
        self.post.save()

        self.post.publish()

        self.assertIsNotNone(
            self.post.published_date
        )


    def test_post_string(self):

        self.assertEqual(
            str(self.post),
            "API GET Test"
        )

    def test_post_new_create(self):

        self.client.force_login(
            self.user
        )

        image = create_test_image(
            "new_post.jpg"
        )

        response = self.client.post(
            "/post/new/",
            {
                "title": "New Post Test",
                "text": "New post creation test",
                "image": image,
            }
        )

        self.assertEqual(
            response.status_code,
            302
        )

        self.assertTrue(
            Post.objects.filter(
                title="New Post Test"
            ).exists()
        )

    def test_post_edit(self):

        self.client.force_login(
            self.user
        )

        image = create_test_image(
            "edit_post.jpg"
        )

        response = self.client.post(
            f"/post/{self.post.id}/edit/",
            {
                "title": "Edited Post",
                "text": "Edited post text",
                "image": image,
            }
        )

        self.assertEqual(
            response.status_code,
            302
        )

        self.post.refresh_from_db()

        self.assertEqual(
            self.post.title,
            "Edited Post"
        )

    def test_post_edit_page(self):

        response = self.client.get(
            f"/post/{self.post.id}/edit/"
        )

        self.assertEqual(
            response.status_code,
            200
        )
    