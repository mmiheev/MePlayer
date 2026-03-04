package com.zeon.meplayer.domain.model

import com.zeon.meplayer.R

enum class SortOption(val displayNameResId: Int) {
    TITLE(R.string.sort_by_title),
    ARTIST(R.string.sort_by_artist),
    DURATION(R.string.sort_by_duration),
    DATE_ADDED(R.string.sort_by_date_added)
}