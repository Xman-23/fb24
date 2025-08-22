package com.example.demo.repository.member.membernotificationsettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.member.membernotificationsettings.MemberNotificationSetting;

@Repository
public interface Membernotificationsettings extends JpaRepository<MemberNotificationSetting, Long>{

}
