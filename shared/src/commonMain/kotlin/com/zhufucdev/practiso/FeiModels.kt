package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.CosineDistanceNormalizer
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.LanguageInput
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.datamodel.TokenInput
import com.zhufucdev.practiso.platform.Language
import usearch.MetricKind
import usearch.ScalarKind

data object JinaV2SmallEn : MlModel(
    hfId = "zhufucdev/jina-embeddings-v2-small-en",
    features = setOf(
        TokenInput(sequenceLength = 512),
        LanguageInput.of(Language.English, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 512u, ScalarKind.F16, CosineDistanceNormalizer),
    )
)

data object DmetaSmallZh : MlModel(
    hfId = "zhufucdev/Dmeta-embedding-zh-small",
    features = setOf(
        TokenInput(sequenceLength = 768),
        LanguageInput.of(Language.English, Language.Chinese, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 768u, ScalarKind.F32, CosineDistanceNormalizer)
    )
)

data object FlagEmbeddingSmallZh : MlModel(
    hfId = "zhufucdev/BAAI-bge-small-zh-v1.5",
    features = setOf(
        TokenInput(sequenceLength = 512),
        LanguageInput.of(Language.English, Language.Chinese, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 512u, ScalarKind.F32, CosineDistanceNormalizer)
    )
)

data object JinaV2EnEs : MlModel(
    hfId = "zhufucdev/jina-embeddings-v2-base-es",
    features = setOf(
        TokenInput(sequenceLength = 768),
        LanguageInput.of(Language.English, Language.Spanish, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 768u, ScalarKind.F16, CosineDistanceNormalizer)
    )
)

data object JinaV2EnDe : MlModel(
    hfId = "zhufucdev/jina-embeddings-v2-base-de",
    features = setOf(
        TokenInput(sequenceLength = 768),
        LanguageInput.of(Language.English, Language.German, Language.Default),
        EmbeddingOutput(MetricKind.Cos, 768u, ScalarKind.F16, CosineDistanceNormalizer)
    )
)

object KnownModels : List<MlModel> by listOf(
    JinaV2SmallEn, DmetaSmallZh,
    FlagEmbeddingSmallZh, JinaV2EnEs, JinaV2EnDe
) {
    operator fun get(hfId: String): MlModel? = firstOrNull { it.hfId == hfId }
}