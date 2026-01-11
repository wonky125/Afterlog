package com.hackathon.afterlog.feature.guide

import androidx.lifecycle.ViewModel
import com.hackathon.afterlog.service.CameraUseCaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    val cameraUseCaseManager: CameraUseCaseManager
) : ViewModel()
