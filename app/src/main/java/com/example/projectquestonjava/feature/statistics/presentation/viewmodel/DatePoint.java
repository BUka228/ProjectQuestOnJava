package com.example.projectquestonjava.feature.statistics.presentation.viewmodel;

import java.time.LocalDate;
import java.util.Objects;

import lombok.Getter;

public record DatePoint(LocalDate date, float value) { }