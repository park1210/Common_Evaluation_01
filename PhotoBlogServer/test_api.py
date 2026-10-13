import requests


API_URL = "http://127.0.0.1:8000/api_root/Post/"


def test_api():

    print("=" * 60)
    print("Photo Blog REST API Automatic Test")
    print("=" * 60)

    # ----------------------------------
    # 1. Server connection
    # ----------------------------------

    print("\n[1] Server Connection")

    try:

        response = requests.get(
            API_URL,
            timeout=5
        )

    except requests.RequestException as e:

        print("FAIL")
        print("Error:", e)
        return


    print(
        "Status Code:",
        response.status_code
    )


    if response.status_code != 200:

        print("FAIL")
        return

    print("PASS")


    # ----------------------------------
    # 2. JSON Array
    # ----------------------------------

    print("\n[2] JSON Validation")

    try:

        data = response.json()

    except ValueError:

        print("FAIL: Invalid JSON")
        return


    if not isinstance(data, list):

        print(
            "FAIL: Response is not JSON Array"
        )

        return


    print("JSON Array: PASS")
    print(
        "Post Count:",
        len(data)
    )


    # ----------------------------------
    # 3. Empty check
    # ----------------------------------

    if len(data) == 0:

        print(
            "\nNo posts found."
        )

        return


    # ----------------------------------
    # 4. Required fields
    # ----------------------------------

    print("\n[3] Required Field Test")


    required_fields = [

        "author",
        "title",
        "text",
        "created_date",
        "published_date",
        "image",

    ]


    first_post = data[0]


    for field in required_fields:

        if field in first_post:

            print(
                field,
                ": PASS"
            )

        else:

            print(
                field,
                ": FAIL"
            )


    # ----------------------------------
    # 5. Image URL
    # ----------------------------------

    print("\n[4] Image URL Test")


    image_url = first_post.get(
        "image"
    )


    if not image_url:

        print(
            "FAIL: Image URL is empty"
        )

        return


    print(
        "Image URL:",
        image_url
    )


    try:

        image_response = requests.get(
            image_url,
            timeout=5
        )

    except requests.RequestException as e:

        print(
            "FAIL:",
            e
        )

        return


    print(
        "Image Status:",
        image_response.status_code
    )


    if image_response.status_code == 200:

        print(
            "Image Download: PASS"
        )

    else:

        print(
            "Image Download: FAIL"
        )


    print()
    print("=" * 60)
    print("AUTOMATIC TEST COMPLETED")
    print("=" * 60)


if __name__ == "__main__":

    test_api()