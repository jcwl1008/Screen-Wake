package hk.jimmy.screenwake;

final class HeyzineAutoFlip {
    private HeyzineAutoFlip() {}

    static String script(boolean advance) {
        return "(function(advance) {\n"
            + "  try {\n"
            + "    const reader = window.heyzine;\n"
            + "    const navigation = reader && reader.controls && reader.controls.navigation;\n"
            + "    if (document.hidden || !reader || !reader.viewer || !reader.config ||\n"
            + "        !navigation || typeof navigation.goToPage !== 'function') return false;\n"
            + "    const current = Number(reader.getCurrentPage());\n"
            + "    if (!Number.isInteger(current) || current < 1) return false;\n"
            + "    if (!advance || Number(reader.config.num_pages) <= 1) return true;\n"
            + "    const start = Number(reader.design && reader.design.data && reader.design.data.start_page);\n"
            + "    const first = Number.isInteger(start) && start >= 1 && start <= reader.config.num_pages ? start : 1;\n"
            + "    const end = Number(reader.design && reader.design.data && reader.design.data.end_page);\n"
            + "    const atConfiguredEnd = end > 0 && reader.getVisiblePages().some(page => page >= end);\n"
            + "    navigation.goToPage(reader.isLastPage() || atConfiguredEnd ? first : 'next', 'button-nav');\n"
            + "    return true;\n"
            + "  } catch (error) { return false; }\n"
            + "})(" + advance + ")";
    }
}
