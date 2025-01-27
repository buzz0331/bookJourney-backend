package com.example.bookjourneybackend.domain.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GetRoomInfoResponse {
    private String bookTitle;
    private String roomName;
    private boolean isPublic;
    private int roomPercentage;
    private String progressEndDate;
    private List<RoomMemberInfo> memberList;

    public static GetRoomInfoResponse of(String bookTitle, String roomName, boolean isPublic, int roomPercentage, String progressEndDate, List<RoomMemberInfo> memberList) {
        return new GetRoomInfoResponse(bookTitle, roomName, isPublic, roomPercentage, progressEndDate, memberList);
    }
}