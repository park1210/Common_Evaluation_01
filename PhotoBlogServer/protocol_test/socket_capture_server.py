import os
import socket
from datetime import datetime


HOST = "127.0.0.1"
PORT = 8000

SAVE_DIR = os.path.join(
    os.path.dirname(__file__),
    "request"
)


def create_request_directory():

    os.makedirs(
        SAVE_DIR,
        exist_ok=True
    )


def receive_http_request(client_socket):

    request_data = b""

    # -----------------------------------
    # HTTP Header 전체 수신
    # -----------------------------------

    while b"\r\n\r\n" not in request_data:

        data = client_socket.recv(4096)

        if not data:
            break

        request_data += data


    # Header를 완전히 받지 못한 경우
    if b"\r\n\r\n" not in request_data:
        return request_data


    header, body = request_data.split(
        b"\r\n\r\n",
        1
    )


    # -----------------------------------
    # Content-Length 확인
    # -----------------------------------

    content_length = 0

    header_text = header.decode(
        "iso-8859-1",
        errors="ignore"
    )


    for line in header_text.split("\r\n"):

        if line.lower().startswith(
            "content-length:"
        ):

            content_length = int(
                line.split(
                    ":",
                    1
                )[1].strip()
            )

            break


    # -----------------------------------
    # Body 전체 수신
    # -----------------------------------

    while len(body) < content_length:

        data = client_socket.recv(4096)

        if not data:
            break

        body += data


    return (
        header
        + b"\r\n\r\n"
        + body
    )


def save_request(request_data):

    timestamp = datetime.now().strftime(
        "%Y-%m-%d-%H-%M-%S"
    )


    # 원본 Binary 저장
    bin_path = os.path.join(
        SAVE_DIR,
        timestamp + ".bin"
    )


    with open(
        bin_path,
        "wb"
    ) as file:

        file.write(
            request_data
        )


    # VS Code에서 보기 쉬운 Text도 저장
    txt_path = os.path.join(
        SAVE_DIR,
        timestamp + ".txt"
    )


    with open(
        txt_path,
        "w",
        encoding="utf-8"
    ) as file:

        file.write(
            request_data.decode(
                "iso-8859-1",
                errors="replace"
            )
        )


    return bin_path, txt_path


def run_server():

    create_request_directory()


    server_socket = socket.socket(
        socket.AF_INET,
        socket.SOCK_STREAM
    )


    server_socket.setsockopt(
        socket.SOL_SOCKET,
        socket.SO_REUSEADDR,
        1
    )


    server_socket.bind(
        (HOST, PORT)
    )


    server_socket.listen(1)


    print(
        "========================================"
    )

    print(
        "Socket Capture Server"
    )

    print(
        f"Listening: {HOST}:{PORT}"
    )

    print(
        "Android에서 '동기화' 버튼을 누르세요."
    )

    print(
        "========================================"
    )


    try:

        client_socket, address = (
            server_socket.accept()
        )


        print(
            "Client connected:",
            address
        )


        request_data = receive_http_request(
            client_socket
        )


        print()
        print(
            request_data.decode(
                "iso-8859-1",
                errors="replace"
            )
        )


        bin_path, txt_path = save_request(
            request_data
        )


        print()
        print(
            "Binary request saved:",
            bin_path
        )

        print(
            "Text request saved:",
            txt_path
        )


        # Android가 JSON을 기대하므로
        # 빈 JSON Array 응답
        body = b"[]"

        response = (
            b"HTTP/1.1 200 OK\r\n"
            b"Content-Type: application/json; charset=utf-8\r\n"
            b"Content-Length: 2\r\n"
            b"Connection: close\r\n"
            b"\r\n"
            + body
        )


        client_socket.sendall(
            response
        )


        client_socket.close()


    finally:

        server_socket.close()


    print()
    print(
        "Request capture completed."
    )


if __name__ == "__main__":

    run_server()