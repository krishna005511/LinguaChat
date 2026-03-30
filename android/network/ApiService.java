package com.nakama.linguachat.network;

import com.nakama.linguachat.models.requests.DirectRoomRequest;
import com.nakama.linguachat.models.requests.LoginRequest;
import com.nakama.linguachat.models.requests.RegisterRequest;
import com.nakama.linguachat.models.requests.ResendOtpRequest;
import com.nakama.linguachat.models.requests.UpdateProfileRequest;
import com.nakama.linguachat.models.requests.VerifyOtpRequest;
import com.nakama.linguachat.models.responses.AuthResponse;
import com.nakama.linguachat.models.responses.MessageResponse;
import com.nakama.linguachat.models.responses.MessagesResponse;
import com.nakama.linguachat.models.responses.RegisterResponse;
import com.nakama.linguachat.models.responses.RoomResponse;
import com.nakama.linguachat.models.responses.RoomsResponse;
import com.nakama.linguachat.models.responses.UserResponse;
import com.nakama.linguachat.models.responses.UsersResponse;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest req);

    @POST("api/auth/verify-otp")
    Call<AuthResponse> verifyOtp(@Body VerifyOtpRequest req);

    @POST("api/auth/resend-otp")
    Call<MessageResponse> resendOtp(@Body ResendOtpRequest req);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest req);

    // ── User ──────────────────────────────────────────────────────────────────
    @GET("api/user/me")
    Call<UserResponse> getMyProfile(@Header("Authorization") String token);

    @PUT("api/user/me")
    Call<UserResponse> updateProfile(@Header("Authorization") String token,
                                     @Body UpdateProfileRequest req);

    @GET("api/user/search")
    Call<UsersResponse> searchUsers(@Header("Authorization") String token,
                                    @Query("q") String query);

    @GET("api/user/{userId}")
    Call<UserResponse> getUserProfile(@Header("Authorization") String token,
                                      @Path("userId") String userId);

    // ── Chat rooms ────────────────────────────────────────────────────────────
    @GET("api/chat/rooms")
    Call<RoomsResponse> getRooms(@Header("Authorization") String token);

    @POST("api/chat/rooms/direct")
    Call<RoomResponse> createDirectRoom(@Header("Authorization") String token,
                                        @Body DirectRoomRequest req);

    @POST("api/chat/rooms/group")
    Call<RoomResponse> createGroupRoom(@Header("Authorization") String token,
                                       @Body Map<String, Object> body);

    // ── Messages ──────────────────────────────────────────────────────────────
    @GET("api/chat/rooms/{roomId}/messages")
    Call<MessagesResponse> getMessages(@Header("Authorization") String token,
                                       @Path("roomId") String roomId,
                                       @Query("limit") int limit,
                                       @Query("before") String before);

    @POST("api/chat/rooms/{roomId}/seen")
    Call<MessageResponse> markRoomSeen(@Header("Authorization") String token,
                                       @Path("roomId") String roomId);

    // ── Media ─────────────────────────────────────────────────────────────────
    @POST("api/media/upload")
    Call<Map<String, Object>> uploadMedia(@Header("Authorization") String token,
                                          @Body Map<String, String> body);
}
