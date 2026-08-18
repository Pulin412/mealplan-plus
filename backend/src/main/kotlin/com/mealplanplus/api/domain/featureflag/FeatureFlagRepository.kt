package com.mealplanplus.api.domain.featureflag

import org.springframework.data.jpa.repository.JpaRepository

interface FeatureFlagRepository : JpaRepository<FeatureFlag, String>
