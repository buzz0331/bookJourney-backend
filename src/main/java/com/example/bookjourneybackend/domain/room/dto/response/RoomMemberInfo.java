package com.example.bookjourneybackend.domain.room.dto.response;

import com.example.bookjourneybackend.domain.userRoom.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomMemberInfo {
    private UserRole userRole;
    private String imageUrl;
    private String nickName;
    private int userPercentage;
}