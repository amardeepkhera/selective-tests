(() => {
    const editorContainer = document.getElementById("questionPaperTagEditor");
    const tagAutocomplete = document.getElementById("tagAutocomplete");
    const tagSearchInput = document.getElementById("tagSearch");
    const selectedTagsContainer = document.getElementById("selectedTags");
    const tagSuggestions = document.getElementById("tagSuggestions");
    const tagsJsonField = document.getElementById("tagsJson");

    if (!editorContainer || !tagAutocomplete || !tagSearchInput || !selectedTagsContainer || !tagSuggestions || !tagsJsonField) {
        return;
    }

    const availableTags = (() => {
        try {
            const parsed = JSON.parse(tagAutocomplete.dataset.availableTags ?? "[]");
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            console.error("Unable to parse available tags", error);
            return [];
        }
    })();
    const selectedTags = new Map();
    const normalizeTagPart = (value) => value.trim().toLowerCase();

    const escapeHtml = (value) => value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");

    const buildTagToken = (tag) => `${normalizeTagPart(tag.key)}::${normalizeTagPart(tag.value)}`;

    const serializeTag = (tag) => (tag.isNew
        ? {key: tag.key, value: tag.value}
        : {id: tag.id, key: tag.key, value: tag.value});

    const syncTagsJson = () => {
        tagsJsonField.value = JSON.stringify(Array.from(selectedTags.values()).map(serializeTag));
    };

    const hideSuggestions = () => {
        tagSuggestions.classList.add("d-none");
        tagSuggestions.replaceChildren();
    };

    const hasSelectedTag = (tag) => Array.from(selectedTags.values())
        .some((selectedTag) => buildTagToken(selectedTag) === buildTagToken(tag));

    const parseTagInput = (value) => {
        const trimmed = value.trim();
        if (!trimmed) {
            return null;
        }

        const separatorIndex = trimmed.indexOf(":");
        if (separatorIndex < 1 || separatorIndex === trimmed.length - 1) {
            return null;
        }

        const key = trimmed.slice(0, separatorIndex).trim();
        const tagValue = trimmed.slice(separatorIndex + 1).trim();
        if (!key || !tagValue) {
            return null;
        }

        return {key, value: tagValue};
    };

    const renderSelectedTags = () => {
        selectedTagsContainer.replaceChildren();

        Array.from(selectedTags.values()).forEach((tag) => {
            const badge = document.createElement("span");
            badge.className = "badge text-bg-secondary d-inline-flex align-items-center gap-2";
            badge.innerHTML = `
                <span>${escapeHtml(`${tag.key}: ${tag.value}`)}</span>
                <button type="button" class="btn-close btn-close-white btn-sm" aria-label="Remove tag"></button>
            `;

            badge.querySelector("button")?.addEventListener("click", () => {
                selectedTags.delete(tag.id);
                syncTagsJson();
                renderSelectedTags();
                renderSuggestions(tagSearchInput.value);
            });

            selectedTagsContainer.appendChild(badge);
        });
    };

    const selectTag = (tag) => {
        selectedTags.set(tag.id, tag);
        syncTagsJson();
        renderSelectedTags();
        tagSearchInput.value = "";
        hideSuggestions();
    };

    const createPendingTag = (value) => {
        const parsedTag = parseTagInput(value);
        if (!parsedTag) {
            return false;
        }

        const existingTag = availableTags.find((tag) => buildTagToken(tag) === buildTagToken(parsedTag));
        if (existingTag) {
            if (!hasSelectedTag(existingTag)) {
                selectTag(existingTag);
            }
            return true;
        }

        if (hasSelectedTag(parsedTag)) {
            tagSearchInput.value = "";
            hideSuggestions();
            return true;
        }

        selectTag({
            id: `new:${buildTagToken(parsedTag)}`,
            key: parsedTag.key,
            value: parsedTag.value,
            isNew: true,
        });
        return true;
    };

    const renderSuggestions = (query, showAllOnEmpty = false) => {
        const normalizedQuery = query.trim().toLowerCase();
        if (!normalizedQuery && !showAllOnEmpty) {
            hideSuggestions();
            return;
        }

        const matches = availableTags
            .filter((tag) => !hasSelectedTag(tag))
            .filter((tag) =>
                !normalizedQuery
                || (
                    tag.key.toLowerCase().includes(normalizedQuery)
                    || tag.value.toLowerCase().includes(normalizedQuery)
                )
            )
            .slice(0, 8);

        tagSuggestions.replaceChildren();

        matches.forEach((tag) => {
            const suggestion = document.createElement("button");
            suggestion.type = "button";
            suggestion.className = "list-group-item list-group-item-action";
            suggestion.textContent = `${tag.key}: ${tag.value}`;
            suggestion.addEventListener("click", () => selectTag(tag));
            tagSuggestions.appendChild(suggestion);
        });

        if (matches.length === 0) {
            const pendingTag = parseTagInput(query);
            if (!pendingTag || hasSelectedTag(pendingTag)) {
                hideSuggestions();
                return;
            }

            const createSuggestion = document.createElement("button");
            createSuggestion.type = "button";
            createSuggestion.className = "list-group-item list-group-item-action";
            createSuggestion.textContent = `Add tag: ${pendingTag.key}: ${pendingTag.value}`;
            createSuggestion.addEventListener("click", () => {
                createPendingTag(query);
            });
            tagSuggestions.appendChild(createSuggestion);
        }

        tagSuggestions.classList.remove("d-none");
    };

    const initializeTags = () => {
        try {
            const parsed = tagsJsonField.value.trim() ? JSON.parse(tagsJsonField.value) : [];
            if (!Array.isArray(parsed)) {
                syncTagsJson();
                return;
            }

            parsed.forEach((entry) => {
                if (typeof entry === "string") {
                    const tag = availableTags.find((item) => item.id === entry);
                    if (tag) {
                        selectedTags.set(tag.id, tag);
                    }
                    return;
                }

                if (entry && typeof entry === "object" && typeof entry.key === "string" && typeof entry.value === "string") {
                    const existingTag = typeof entry.id === "string"
                        ? availableTags.find((item) => item.id === entry.id)
                        : availableTags.find((item) => buildTagToken(item) === buildTagToken(entry));
                    if (existingTag) {
                        selectedTags.set(existingTag.id, existingTag);
                        return;
                    }

                    selectedTags.set(`new:${buildTagToken(entry)}`, {
                        id: `new:${buildTagToken(entry)}`,
                        key: entry.key,
                        value: entry.value,
                        isNew: true,
                    });
                }
            });
        } catch (error) {
            console.error("Unable to parse initial tags JSON", error);
        }

        syncTagsJson();
        renderSelectedTags();
    };

    tagSearchInput.addEventListener("input", (event) => {
        renderSuggestions(event.target.value);
    });
    tagSearchInput.addEventListener("focus", () => {
        renderSuggestions(tagSearchInput.value);
    });
    tagSearchInput.addEventListener("keydown", (event) => {
        if (event.key !== "Enter") {
            return;
        }

        const hasVisibleSuggestions = !tagSuggestions.classList.contains("d-none");
        const firstSuggestion = tagSuggestions.querySelector("button");
        const selected = createPendingTag(tagSearchInput.value);

        if (selected) {
            event.preventDefault();
            return;
        }

        if (hasVisibleSuggestions && firstSuggestion) {
            event.preventDefault();
            firstSuggestion.click();
        }
    });
    tagSearchInput.addEventListener("dblclick", () => {
        renderSuggestions("", true);
    });
    document.addEventListener("click", (event) => {
        if (!tagAutocomplete.contains(event.target)) {
            hideSuggestions();
        }
    });

    initializeTags();
})();
