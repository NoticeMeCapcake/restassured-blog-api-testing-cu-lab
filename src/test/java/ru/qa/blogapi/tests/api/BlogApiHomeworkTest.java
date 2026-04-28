package ru.qa.blogapi.tests.api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qa.blogapi.auth.AuthApiClient;
import ru.qa.blogapi.auth.AuthSession;
import ru.qa.blogapi.base.BaseAuthorizedApiTest;
import ru.qa.blogapi.models.PostCreateRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class BlogApiHomeworkTest extends BaseAuthorizedApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/auth/register -> should register user with valid required fields")
    void shouldRegisterUserWithValidRequiredFields() {
        String email = randomEmail();
        String password = "SecurePass123!";

        Map<String, Object> body = registrationBody(email, password);

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", equalTo("User registered successfully"))
                .body("user.id", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.firstName", equalTo(body.get("firstName")))
                .body("user.lastName", equalTo(body.get("lastName")))
                .body("user.nickname", equalTo(body.get("nickname")))
                .body("user.birthDate", equalTo(body.get("birthDate")))
                .body("user.phone", equalTo(body.get("phone")));
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/auth/register -> should return validation error for invalid email")
    void shouldReturnValidationErrorForInvalidEmailOnRegistration() {
        Map<String, Object> body = registrationBody("invalid-email", "SecurePass123!");

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue())
                .body("error.code", equalTo(400))
                .body("error.message", notNullValue());
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/login -> should login with valid credentials")
    void shouldLoginWithValidCredentials() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUser(email, password);

        given()
                .spec(requestSpec)
                .body(loginBody(email, password))
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refresh_token", notNullValue());
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/login -> should return unauthorized for wrong password")
    void shouldReturnUnauthorizedForWrongPassword() {
        String email = randomEmail();
        String password = "SecurePass123!";
        String wrongPassword = "WrongPass123!";

        registerUser(email, password);

        given()
                .spec(requestSpec)
                .body(loginBody(email, wrongPassword))
                .when()
                .post("/api/login")
                .then()
                .statusCode(401)
                .body("code", equalTo(401))
                .body("message", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("POST /api/token/refresh -> should refresh access token by refresh token")
    void shouldRefreshAccessToken() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUser(email, password);

        Response loginResponse = given()
                .spec(requestSpec)
                .body(loginBody(email, password))
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String refreshToken = loginResponse.jsonPath().getString("refresh_token");

        given()
                .spec(requestSpec)
                .body(Map.of("refresh_token", refreshToken))
                .when()
                .post("/api/token/refresh")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refresh_token", notNullValue());
    }

    @Test
    @Tag("smoke")
    @DisplayName("GET /api/profile -> should return current user profile for authorized user")
    void shouldReturnCurrentUserProfile() {
        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/profile")
                .then()
                .statusCode(200)
                .body("user", notNullValue())
                .body("user.id", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("PUT /api/profile -> should update current user profile")
    void shouldUpdateCurrentUserProfile() {
        String newFirstName = "Updated" + suffix(5);
        String newLastName = "User" + suffix(5);
        String newNickname = "updated" + suffix(5);
        String newPhone = randomPhone();

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("firstName", newFirstName);
        updateBody.put("lastName", newLastName);
        updateBody.put("nickname", newNickname);
        updateBody.put("phone", newPhone);

        given()
                .spec(authorizedRequestSpec)
                .body(updateBody)
                .when()
                .put("/api/profile")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("user.firstName", equalTo(newFirstName))
                .body("user.lastName", equalTo(newLastName))
                .body("user.nickname", equalTo(newNickname))
                .body("user.phone", equalTo(newPhone));
    }

    @Test
    @Tag("smoke")
    @DisplayName("GET /api/posts -> should return paginated list of posts")
    void shouldReturnPaginatedPostsList() {
        given()
                .spec(authorizedRequestSpec)
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("totalItems", notNullValue())
                .body("itemsPerPage", equalTo(10))
                .body("page", equalTo(1))
                .body("pages", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/posts -> should filter posts by category")
    void shouldFilterPostsByCategory() {
        String technologyCategory = "technology";
        String foodCategory = "food";

        PostCreateRequest techPost = new PostCreateRequest(
                "Tech Post " + suffix(5),
                "Tech body",
                "Tech description",
                technologyCategory,
                false
        );

        PostCreateRequest foodsPost = new PostCreateRequest(
                "foods Post " + suffix(5),
                "foods body",
                "foods description",
                foodCategory,
                false
        );

        given()
                .spec(authorizedRequestSpec)
                .body(techPost)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .body(foodsPost)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .queryParam("category", technologyCategory)
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.every { it.category == '" + technologyCategory + "' }", equalTo(true));
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/posts -> should create published post")
    void shouldCreatePublishedPost() {
        String title = "Published Post " + suffix(5);
        String body = "Published post body";
        String description = "Published post description";
        String category = "technology";

        PostCreateRequest requestBody = new PostCreateRequest(
                title,
                body,
                description,
                category,
                false
        );

        given()
                .spec(authorizedRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .body("status", equalTo("success"))
                .body("post.id", notNullValue())
                .body("post.title", equalTo(title))
                .body("post.isDraft", equalTo(false))
                .body("post.author.email", equalTo(authSession.getEmail()));
    }

    @Test
    @Tag("regression")
    @DisplayName("POST /api/posts -> should create draft post")
    void shouldCreateDraftPost() {
        String title = "Draft Post " + suffix(5);
        String body = "Draft post body";
        String description = "Draft post description";
        String category = "technology";

        PostCreateRequest requestBody = new PostCreateRequest(
                title,
                body,
                description,
                category,
                true
        );

        given()
                .spec(authorizedRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .body("post.id", notNullValue())
                .body("post.isDraft", equalTo(true))
                .body("post.title", equalTo(title))
                .body("post.author.email", equalTo(authSession.getEmail()));
    }

    @Test
    @Tag("smoke")
    @DisplayName("GET /api/posts/my -> should return only current user posts")
    void shouldReturnOnlyCurrentUserPosts() {
        PostCreateRequest post1 = new PostCreateRequest(
                "My Post 1 " + suffix(5),
                "Body 1",
                "Description 1",
                "technology",
                false
        );

        PostCreateRequest post2 = new PostCreateRequest(
                "My Post 2 " + suffix(5),
                "Body 2",
                "Description 2",
                "food",
                false
        );

        given()
                .spec(authorizedRequestSpec)
                .body(post1)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .body(post2)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/my")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.every { it.author.email == '" + authSession.getEmail() + "' }", equalTo(true));
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/posts/feed -> should return posts from other users")
    void shouldReturnFeedPosts() {
        AuthApiClient authApiClient = new AuthApiClient(requestSpec);
        AuthSession otherUserSession = authApiClient.createAuthorizedSession();
        RequestSpecification otherUserSpec = new io.restassured.builder.RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + otherUserSession.getAccessToken())
                .build();

        PostCreateRequest otherUserPost = new PostCreateRequest(
                "Other User Post " + suffix(5),
                "Other user body",
                "Other user description",
                "technology",
                false
        );

        given()
                .spec(otherUserSpec)
                .body(otherUserPost)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        PostCreateRequest currentUserPost = new PostCreateRequest(
                "Current User Post " + suffix(5),
                "Current user body",
                "Current user description",
                "food",
                false
        );

        given()
                .spec(authorizedRequestSpec)
                .body(currentUserPost)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201);

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/feed")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.every { it.author.email != '" + authSession.getEmail() + "' }", equalTo(true));
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/posts/{id} -> should return single post by id")
    void shouldReturnSinglePostById() {
        String title = "Single Post " + suffix(5);
        String body = "Single post body";
        String description = "Single post description";
        String category = "technology";

        PostCreateRequest requestBody = new PostCreateRequest(
                title,
                body,
                description,
                category,
                false
        );

        Response createResponse = given()
                .spec(authorizedRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer postId = createResponse.jsonPath().getInt("post.id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .when()
                .get("/api/posts/{id}")
                .then()
                .statusCode(200)
                .body("post.id", equalTo(postId))
                .body("post.title", equalTo(title))
                .body("post.body", equalTo(body))
                .body("statistics", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("PUT /api/posts/{id} -> should update existing post")
    void shouldUpdateExistingPost() {
        String originalTitle = "Original Post " + suffix(5);
        String originalDescription = "Original description";

        PostCreateRequest originalPost = new PostCreateRequest(
                originalTitle,
                "Original body",
                originalDescription,
                "technology",
                false
        );

        Response createResponse = given()
                .spec(authorizedRequestSpec)
                .body(originalPost)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer postId = createResponse.jsonPath().getInt("post.id");

        String updatedTitle = "Updated Post " + suffix(5);
        String updatedDescription = "Updated description";

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("title", updatedTitle);
        updateBody.put("description", updatedDescription);

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .body(updateBody)
                .when()
                .put("/api/posts/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("post.id", equalTo(postId))
                .body("post.title", equalTo(updatedTitle))
                .body("post.description", equalTo(updatedDescription));
    }

    @Test
    @Tag("e2e")
    @DisplayName("DELETE /api/posts/{id} -> should delete post")
    void shouldDeletePost() {
        PostCreateRequest requestBody = new PostCreateRequest(
                "Post to Delete " + suffix(5),
                "Post body to delete",
                "Post description to delete",
                "technology",
                false
        );

        Response createResponse = given()
                .spec(authorizedRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer postId = createResponse.jsonPath().getInt("post.id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .when()
                .delete("/api/posts/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", notNullValue());

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .when()
                .get("/api/posts/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @Tag("regression")
    @DisplayName("POST /api/posts/{id}/favorite -> should add post to favorites")
    void shouldAddPostToFavorites() {
        PostCreateRequest requestBody = new PostCreateRequest(
                "Favorite Post " + suffix(5),
                "Favorite post body",
                "Favorite post description",
                "technology",
                false
        );

        Response createResponse = given()
                .spec(authorizedRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer postId = createResponse.jsonPath().getInt("post.id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .body(Map.of("isFavorite", true))
                .when()
                .post("/api/posts/{id}/favorite")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("isFavorite", equalTo(true));
    }

    @Test
    @Tag("e2e")
    @DisplayName("GET /api/posts/favorites -> should return favorite posts")
    void shouldReturnFavoritePosts() {
        PostCreateRequest requestBody = new PostCreateRequest(
                "Favorite Post " + suffix(5),
                "Favorite post body",
                "Favorite post description",
                "technology",
                false
        );

        Response createResponse = given()
                .spec(authorizedRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer postId = createResponse.jsonPath().getInt("post.id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", postId)
                .body(Map.of("isFavorite", true))
                .when()
                .post("/api/posts/{id}/favorite")
                .then()
                .statusCode(200);

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/favorites")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.any { it.id == " + postId + " }", equalTo(true));
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/files/upload -> should upload image file for post")
    void shouldUploadImageFileForPost() throws IOException {
        java.io.File testFile = new java.io.File(getClass().getClassLoader().getResource("test.png").getFile());

        var testFileBytes = Files.readAllBytes(testFile.toPath());

        given()
                .spec(authorizedRequestSpec)
                .contentType("multipart/form-data")
                .multiPart("file", "test.png", testFileBytes, "image/png")
                .multiPart("type", "post-image")
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("url", notNullValue())
                .body("mimeType", notNullValue())
                .body("filename", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/files/{id} -> should return uploaded file metadata")
    void shouldReturnUploadedFileMetadata() throws IOException{
        java.io.File testFile = new java.io.File(getClass().getClassLoader().getResource("test.png").getFile());

        var testFileBytes = Files.readAllBytes(testFile.toPath());

        Response uploadResponse = given()
                .spec(authorizedRequestSpec)
                .contentType("multipart/form-data")
                .multiPart("file", "test.png", testFileBytes, "image/png")
                .multiPart("type", "post-image")
                .when()
                .post("/api/files/upload")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("url", notNullValue())
                .body("mimeType", notNullValue())
                .body("filename", notNullValue())
                .extract()
                .response();

        String fileId = uploadResponse.jsonPath().getString("id");

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", fileId)
                .when()
                .get("/api/files/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(fileId))
                .body("url", notNullValue())
                .body("filename", notNullValue())
                .body("size", notNullValue())
                .body("mimeType", notNullValue());
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/profile/report/{id} -> should create report for user")
    void shouldCreateUserReport() {
        AuthApiClient authApiClient = new AuthApiClient(requestSpec);
        AuthSession otherUserSession = authApiClient.createAuthorizedSession();
        Integer otherUserId = otherUserSession.getUserId();

        Map<String, Object> reportBody = new HashMap<>();
        reportBody.put("descriptionReport", "This is a test report for user " + otherUserId);

        given()
                .spec(authorizedRequestSpec)
                .pathParam("id", otherUserId)
                .body(reportBody)
                .when()
                .post("/api/profile/report/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", notNullValue());
    }

    private Response registerUser(String email, String password) {
        return given()
                .spec(requestSpec)
                .body(registrationBody(email, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private Map<String, Object> registrationBody(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("firstName", "Ronam");
        body.put("lastName", "Doe");
        body.put("nickname", "roman_" + suffix(5));
        body.put("birthDate", "1990-01-02");
        body.put("phone", randomPhone());
        return body;
    }

    private Map<String, Object> loginBody(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", email);
        body.put("password", password);
        return body;
    }

    private String randomEmail() {
        return "student_" + suffix(8) + "@example.com";
    }

    private String randomPhone() {
        return "+79" + UUID.randomUUID()
                .toString()
                .replaceAll("[^0-9]", "")
                .substring(0, 9);
    }

    private String suffix(int length) {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, length);
    }
}