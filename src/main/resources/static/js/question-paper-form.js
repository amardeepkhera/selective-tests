(() => {
    const editorContainer = document.getElementById("questionPaperEditor");
    const tagAutocomplete = document.getElementById("tagAutocomplete");
    const tagSearchInput = document.getElementById("tagSearch");
    const selectedTagsContainer = document.getElementById("selectedTags");
    const tagSuggestions = document.getElementById("tagSuggestions");
    const tagsJsonField = document.getElementById("tagsJson");
    const searchQuestionsButton = document.getElementById("searchQuestionsButton");
    const questionSearchMessage = document.getElementById("questionSearchMessage");
    const questionPaperWorkflow = document.getElementById("questionPaperWorkflow");
    const questionSearchResults = document.getElementById("questionSearchResults");
    const questionSearchResultsBody = document.getElementById("questionSearchResultsBody");
    const questionSelectAllCheckbox = document.getElementById("questionSelectAllCheckbox");
    const questionSearchLoading = document.getElementById("questionSearchLoading");
    const questionSearchEnd = document.getElementById("questionSearchEnd");

    if (!editorContainer || !tagAutocomplete || !tagSearchInput || !selectedTagsContainer || !tagSuggestions || !tagsJsonField || !searchQuestionsButton || !questionSearchMessage || !questionPaperWorkflow || !questionSearchResults || !questionSearchResultsBody || !questionSelectAllCheckbox || !questionSearchLoading || !questionSearchEnd) {
        return;
    }

    const QUESTION_PAGE_SIZE = 10;
    const QUESTION_SCROLL_THRESHOLD = 16;

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
    const questionSearchState = {
        tagIds: [],
        page: 0,
        hasMore: false,
        isLoading: false,
        requestToken: 0,
    };
    const normalizeTagPart = (value) => value.trim().toLowerCase();

    const escapeHtml = (value) => value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");

    const serializeTag = (tag) => tag.id;

    const syncTagsJson = () => {
        tagsJsonField.value = JSON.stringify(Array.from(selectedTags.values()).map(serializeTag));
    };

    const hideSuggestions = () => {
        tagSuggestions.classList.add("d-none");
        tagSuggestions.replaceChildren();
    };

    const showMessage = (message, type) => {
        questionSearchMessage.className = `alert alert-${type}`;
        questionSearchMessage.textContent = message;
        questionSearchMessage.classList.remove("d-none");
    };

    const hideMessage = () => {
        questionSearchMessage.classList.add("d-none");
        questionSearchMessage.textContent = "";
    };

    const hideResults = () => {
        questionPaperWorkflow.classList.add("d-none");
        questionSearchResults.classList.add("d-none");
        questionSearchResultsBody.replaceChildren();
        questionSearchResults.scrollTop = 0;
        questionSelectAllCheckbox.checked = false;
        questionSelectAllCheckbox.indeterminate = false;
        questionSearchLoading.classList.add("d-none");
        questionSearchEnd.classList.add("d-none");
    };

    const resetQuestionSearchState = () => {
        questionSearchState.tagIds = [];
        questionSearchState.page = 0;
        questionSearchState.hasMore = false;
        questionSearchState.isLoading = false;
        questionSearchState.requestToken += 1;
    };

    const updateSelectAllCheckboxState = () => {
        const rowCheckboxes = Array.from(questionSearchResultsBody.querySelectorAll(".question-row-checkbox"));
        if (rowCheckboxes.length === 0) {
            questionSelectAllCheckbox.checked = false;
            questionSelectAllCheckbox.indeterminate = false;
            return;
        }

        const checkedCount = rowCheckboxes.filter((checkbox) => checkbox.checked).length;
        questionSelectAllCheckbox.checked = checkedCount === rowCheckboxes.length;
        questionSelectAllCheckbox.indeterminate = checkedCount > 0 && checkedCount < rowCheckboxes.length;
    };

    const buildTagToken = (tag) => `${normalizeTagPart(tag.key)}::${normalizeTagPart(tag.value)}`;

    const hasSelectedTag = (tag) => Array.from(selectedTags.values())
        .some((selectedTag) => buildTagToken(selectedTag) === buildTagToken(tag));

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
            hideSuggestions();
            return;
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
                    const existingTag = availableTags.find((item) => buildTagToken(item) === buildTagToken(entry));
                    if (existingTag) {
                        selectedTags.set(existingTag.id, existingTag);
                    }
                }
            });
        } catch (error) {
            console.error("Unable to parse initial tags JSON", error);
        }

        syncTagsJson();
        renderSelectedTags();
    };

    const createQuestionRow = (question) => {
        const row = document.createElement("tr");
        row.className = "question-result-row";

        const checkboxCell = document.createElement("td");
        checkboxCell.className = "question-select-cell";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.className = "form-check-input question-row-checkbox";
        checkbox.name = "selectedQuestionIds";
        checkbox.value = question.id ?? "";
        checkbox.setAttribute("aria-label", "Select question");
        checkbox.checked = questionSelectAllCheckbox.checked;
        checkbox.addEventListener("change", updateSelectAllCheckboxState);
        checkboxCell.appendChild(checkbox);

        const textCell = document.createElement("td");
        textCell.className = "question-result-cell";

        const layout = document.createElement("div");
        layout.className = "question-result-layout";

        const content = document.createElement("div");
        content.className = "question-result-content";

        const fullText = document.createElement("div");
        fullText.className = "question-text-full";
        fullText.innerHTML = question.text ?? "";

        content.appendChild(fullText);
        layout.append(content);
        row.appendChild(checkboxCell);
        textCell.appendChild(layout);
        row.appendChild(textCell);

        return row;
    };

    const renderQuestionResults = (questions, { append = false } = {}) => {
        if (!append) {
            questionSearchResultsBody.replaceChildren();
        }

        questions.forEach((question) => {
            questionSearchResultsBody.appendChild(createQuestionRow(question));
        });

        questionPaperWorkflow.classList.remove("d-none");
        questionSearchResults.classList.remove("d-none");
        updateSelectAllCheckboxState();
    };

    const updateQuestionSearchStatus = () => {
        questionSearchLoading.classList.toggle("d-none", !questionSearchState.isLoading);
        const shouldShowEnd = !questionSearchState.isLoading
            && !questionSearchState.hasMore
            && questionSearchResultsBody.childElementCount > 0;
        questionSearchEnd.classList.toggle("d-none", !shouldShowEnd);
    };

    const isNearResultsBottom = () => questionSearchResults.scrollTop + questionSearchResults.clientHeight
        >= questionSearchResults.scrollHeight - QUESTION_SCROLL_THRESHOLD;

    const loadQuestionsPage = async ({ reset = false } = {}) => {
        const tagIds = reset ? Array.from(selectedTags.keys()) : questionSearchState.tagIds;
        if (tagIds.length === 0) {
            resetQuestionSearchState();
            hideResults();
            showMessage("Select at least one tag to search questions.", "warning");
            return;
        }

        if (questionSearchState.isLoading && !reset) {
            return;
        }

        if (!reset && !questionSearchState.hasMore) {
            return;
        }

        const requestToken = reset ? questionSearchState.requestToken + 1 : questionSearchState.requestToken;
        if (reset) {
            questionSearchState.tagIds = tagIds;
            questionSearchState.page = 0;
            questionSearchState.hasMore = false;
            questionSearchState.requestToken = requestToken;
            hideResults();
        }

        hideMessage();
        questionSearchState.isLoading = true;
        updateQuestionSearchStatus();

        if (reset) {
            searchQuestionsButton.disabled = true;
            searchQuestionsButton.textContent = "Searching...";
        }

        try {
            const params = new URLSearchParams();
            tagIds.forEach((tagId) => params.append("tagIds", tagId));
            params.set("page", String(questionSearchState.page));
            params.set("size", String(QUESTION_PAGE_SIZE));

            const response = await fetch(`/question-paper-form/questions?${params.toString()}`, {
                headers: {
                    Accept: "application/json",
                },
            });

            if (!response.ok) {
                throw new Error(`Request failed with status ${response.status}`);
            }

            const result = await response.json();
            if (questionSearchState.requestToken !== requestToken) {
                return;
            }

            const questions = Array.isArray(result?.questions) ? result.questions : [];
            if (reset && questions.length === 0) {
                resetQuestionSearchState();
                hideResults();
                showMessage("No questions found for the selected tags.", "secondary");
                return;
            }

            hideMessage();
            renderQuestionResults(questions, { append: !reset });
            questionSearchState.page += 1;
            questionSearchState.hasMore = Boolean(result?.hasMore);
            updateQuestionSearchStatus();

        } catch (error) {
            console.error("Unable to fetch questions", error);
            if (reset) {
                resetQuestionSearchState();
                hideResults();
            }
            showMessage("Unable to load questions right now.", "danger");
        } finally {
            if (questionSearchState.requestToken === requestToken) {
                questionSearchState.isLoading = false;
                updateQuestionSearchStatus();
            }

            if (reset) {
                searchQuestionsButton.disabled = false;
                searchQuestionsButton.textContent = "Search Questions";
            }
        }
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
    searchQuestionsButton.addEventListener("click", () => {
        void loadQuestionsPage({ reset: true });
    });
    questionSelectAllCheckbox.addEventListener("change", () => {
        const rowCheckboxes = questionSearchResultsBody.querySelectorAll(".question-row-checkbox");
        rowCheckboxes.forEach((checkbox) => {
            checkbox.checked = questionSelectAllCheckbox.checked;
        });
        questionSelectAllCheckbox.indeterminate = false;
    });
    questionSearchResults.addEventListener("scroll", () => {
        const isScrollable = questionSearchResults.scrollHeight > questionSearchResults.clientHeight;
        if (!questionSearchResults.classList.contains("d-none") && isScrollable && isNearResultsBottom()) {
            void loadQuestionsPage();
        }
    });

    initializeTags();
})();
