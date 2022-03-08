package com.bdo.model.telegram;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ButtonCallbackData {
    @JsonProperty(value = "_i")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long itemId;
    @JsonProperty(value = "_e")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer enhancement;
    @JsonProperty(value = "_a")
    private ButtonAction action;

    public ButtonCallbackData(Long itemId, ButtonAction action) {
        this.itemId = itemId;
        this.action = action;
    }
}
