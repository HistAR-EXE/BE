package com.histar.be.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "quest_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "quest_id", nullable = false)
    @ToString.Exclude
    private Quest quest;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(name = "unlock_key", nullable = false)
    private String unlockKey;

    private String title;

    private String objective;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String hint;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "action_label")
    private String actionLabel;

    @Column(name = "xp_partial")
    private Integer xpPartial;

    @Column(name = "chat_prompt", columnDefinition = "text")
    private String chatPrompt;

    @Column(name = "portal_era")
    private Integer portalEra;

    @Column(name = "preview_image")
    private String previewImage;
}