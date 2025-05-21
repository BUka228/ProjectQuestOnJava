package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationState;
import com.example.projectquestonjava.core.data.model.core.Tag;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CombinedState {
    private TaskCreationState taskState;
    private Loadable<List<Tag>> tags;
}