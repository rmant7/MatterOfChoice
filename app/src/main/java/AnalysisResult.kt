import kotlinx.serialization.Serializable


@Serializable
class AnalysisResult(
    val overall_judgement: String,
    val cases: List<Case>
)

@Serializable
data class Case(
    val case_description: String,
    val player_choice: String,
    val optimal_choice: String,
    val analysis: String
)
