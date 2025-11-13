package com.softcode.nextstep.domain.journey;

import com.softcode.nextstep.domain.BaseEntity;
import com.softcode.nextstep.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "journeys")
public class Journey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "desired_job", nullable = false, length = 150)
    private String desiredJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JourneyStatus status = JourneyStatus.ACTIVE;

    @Column(name = "overall_progress", nullable = false)
    private int overallProgress;

    @Column(name = "estimated_time", length = 80)
    private String estimatedTime;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime completedAt;

    @Lob
    @Column(name = "insights_json", columnDefinition = "CLOB")
    private String insightsJson;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<JourneyStep> steps = new ArrayList<>();
}
