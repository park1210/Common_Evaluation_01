from datetime import timedelta

from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone

from rest_framework import viewsets, filters, status
from rest_framework.response import Response

from .models import Post
from .forms import PostForm
from .serializers import PostSerializer


# =========================================================
# 일반 웹 페이지
# =========================================================

def post_list(request):
    posts = Post.objects.filter(
        published_date__lte=timezone.now()
    ).order_by('published_date')

    return render(
        request,
        'blog/post_list.html',
        {
            'posts': posts
        }
    )


def post_detail(request, pk):
    post = get_object_or_404(
        Post,
        pk=pk
    )

    return render(
        request,
        'blog/post_detail.html',
        {
            'post': post
        }
    )


def post_new(request):
    if request.method == "POST":
        form = PostForm(
            request.POST,
            request.FILES
        )

        if form.is_valid():
            post = form.save(
                commit=False
            )

            post.author = request.user
            post.published_date = timezone.now()
            post.save()

            return redirect(
                'post_detail',
                pk=post.pk
            )

    else:
        form = PostForm()


    return render(
        request,
        'blog/post_edit.html',
        {
            'form': form
        }
    )


def post_edit(request, pk):
    post = get_object_or_404(
        Post,
        pk=pk
    )

    if request.method == "POST":
        form = PostForm(
            request.POST,
            request.FILES,
            instance=post
        )

        if form.is_valid():
            post = form.save(
                commit=False
            )

            post.author = request.user
            post.published_date = timezone.now()
            post.save()

            return redirect(
                'post_detail',
                pk=post.pk
            )

    else:
        form = PostForm(
            instance=post
        )


    return render(
        request,
        'blog/post_edit.html',
        {
            'form': form
        }
    )


def js_test(request):
    return render(
        request,
        'blog/js_test.html',
        {}
    )


# =========================================================
# REST API
# =========================================================

class BlogImages(
    viewsets.ModelViewSet
):

    queryset = Post.objects.all()
    serializer_class = PostSerializer


    # =====================================================
    # 추가 기능 3
    # Server-side 검색 / 정렬
    # =====================================================

    filter_backends = [
        filters.SearchFilter,
        filters.OrderingFilter,
    ]


    # 제목 + 본문 검색
    search_fields = [
        'title',
        'text',
    ]


    # 정렬 가능 필드
    ordering_fields = [
        'created_date',
        'published_date',
        'title',
    ]


    # 기본 정렬 = 최신 작성순
    ordering = [
        '-created_date'
    ]


    # =====================================================
    # 추가 기능 5
    # 중복 게시물 생성 방지
    # =====================================================

    def create(
        self,
        request,
        *args,
        **kwargs
    ):

        # -------------------------------------------------
        # POST 데이터 읽기
        # -------------------------------------------------

        author_id = request.data.get(
            'author'
        )

        title = request.data.get(
            'title',
            ''
        ).strip()

        text = request.data.get(
            'text',
            ''
        ).strip()


        # -------------------------------------------------
        # 중복 검사 기준 시간
        #
        # 최근 10초 안에
        # 같은 작성자 + 제목 + 본문이면
        # 중복으로 판단
        # -------------------------------------------------

        duplicate_since = (
            timezone.now()
            - timedelta(seconds=10)
        )


        duplicate_post = (
            Post.objects
            .filter(
                author_id=author_id,
                title=title,
                text=text,
                created_date__gte=duplicate_since
            )
            .order_by(
                '-created_date'
            )
            .first()
        )


        # -------------------------------------------------
        # 중복 게시물 발견
        # -------------------------------------------------

        if duplicate_post is not None:

            return Response(
                {
                    'detail':
                        'Duplicate post prevented.',

                    'message':
                        '동일한 게시물이 최근에 이미 등록되었습니다.',

                    'post_id':
                        duplicate_post.pk
                },
                status=status.HTTP_409_CONFLICT
            )


        # -------------------------------------------------
        # 중복이 아니라면
        # 기존 DRF POST 생성 로직 실행
        # -------------------------------------------------

        return super().create(
            request,
            *args,
            **kwargs
        )