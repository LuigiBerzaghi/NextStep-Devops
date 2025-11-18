package com.softcode.nextstep.domain.resume;

import com.softcode.nextstep.domain.BaseEntity;
import com.softcode.nextstep.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resume_analysis")
public class ResumeAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    @Column(name = "skills_json", nullable = false)
    private String skillsJson;

    @Lob
    @Column(name = "gaps_json", nullable = false)
    private String gapsJson;

    @Lob
    @Column(name = "suggested_careers_json", nullable = false)
    private String suggestedCareersJson;

    @Column(name = "experience_level", length = 80)
    private String experienceLevel;

    @Column(name = "current_job", length = 150)
    private String currentJob;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
}
