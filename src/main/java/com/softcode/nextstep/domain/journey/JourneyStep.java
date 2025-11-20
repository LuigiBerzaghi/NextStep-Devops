package com.softcode.nextstep.domain.journey;

import com.softcode.nextstep.domain.BaseEntity;
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
@Table(name = "journey_steps")
public class JourneyStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "journey_id")
    private Journey journey;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String objective;

    @Column(length = 500)
    private String resources;

    @Lob
    @Column(name = "platforms_json")
    private String platformsJson;

    @Column(name = "estimated_time", length = 80)
    private String estimatedTime;

    @Column(nullable = false)
    private boolean progress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JourneyStepStatus status = JourneyStepStatus.PENDING;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}
