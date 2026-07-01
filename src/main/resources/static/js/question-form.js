(() => {
    const holder = document.getElementById("textEditor");
    const textarea = document.getElementById("text");
    const form = textarea?.closest("form");
    const validationMessage = document.getElementById("textValidationMessage");
    const optionsContainer = document.getElementById("optionBuilder");
    const addOptionButton = document.getElementById("addOptionButton");
    const optionsJsonField = document.getElementById("optionsJson");
    const optionsValidationMessage = document.getElementById("optionsValidationMessage");
    const tagAutocomplete = document.getElementById("tagAutocomplete");
    const tagSearchInput = document.getElementById("tagSearch");
    const selectedTagsContainer = document.getElementById("selectedTags");
    const tagSuggestions = document.getElementById("tagSuggestions");
    const tagsJsonField = document.getElementById("tagsJson");
    const initialHtml = holder?.dataset.initialHtml ?? "";
    const listTool = window.EditorjsList || window.List;

    if (!holder || !textarea || !form || !validationMessage || !optionsContainer || !addOptionButton || !optionsJsonField || !optionsValidationMessage || !tagAutocomplete || !tagSearchInput || !selectedTagsContainer || !tagSuggestions || !tagsJsonField || typeof EditorJS === "undefined") {
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

    const buildInitialData = (html) => {
        const trimmed = (html || "").trim();
        if (!trimmed) {
            return undefined;
        }

        return {
            blocks: [
                {
                    type: "paragraph",
                    data: {
                        text: trimmed,
                    },
                },
            ],
        };
    };

    const escapeHtml = (value) => value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");

    const renderListItems = (items) => items
        .map((item) => {
            if (typeof item === "string") {
                return `<li>${item}</li>`;
            }

            if (item && typeof item === "object") {
                const content = item.content ?? "";
                const nestedItems = Array.isArray(item.items) && item.items.length > 0
                    ? renderList(item.meta?.style ?? "unordered", item.items)
                    : "";
                return `<li>${content}${nestedItems}</li>`;
            }

            return "";
        })
        .join("");

    const renderList = (style, items) => {
        const tag = style === "ordered" ? "ol" : "ul";
        return `<${tag}>${renderListItems(items)}</${tag}>`;
    };

    const renderBlock = (block) => {
        if (block.type === "paragraph") {
            return `<p>${block.data?.text ?? ""}</p>`;
        }

        if (block.type === "header") {
            const level = Number(block.data?.level) || 2;
            const normalizedLevel = Math.min(Math.max(level, 1), 6);
            return `<h${normalizedLevel}>${block.data?.text ?? ""}</h${normalizedLevel}>`;
        }

        if (block.type === "list") {
            const items = Array.isArray(block.data?.items) ? block.data.items : [];
            return renderList(block.data?.style, items);
        }

        return `<p>${escapeHtml(JSON.stringify(block.data ?? {}))}</p>`;
    };

    const editorConfig = {
        holder: "textEditor",
        minHeight: 160,
        autofocus: true,
        placeholder: "Write the question text here",
        inlineToolbar: ["bold", "italic", "link"],
        data: buildInitialData(initialHtml),
    };

    if (listTool) {
        editorConfig.tools = {
            list: {
                class: listTool,
                inlineToolbar: true,
                config: {
                    defaultStyle: "unordered",
                },
            },
        };
    }

    const editor = new EditorJS(editorConfig);

    const syncEditorValue = async () => {
        const output = await editor.save();
        textarea.value = output.blocks.map(renderBlock).join("").trim();
    };

    const showValidationError = () => {
        holder.classList.add("is-invalid");
        validationMessage.classList.remove("d-none");
    };

    const clearValidationError = () => {
        holder.classList.remove("is-invalid");
        validationMessage.classList.add("d-none");
    };

    const showOptionsValidationError = () => {
        optionsContainer.classList.add("is-invalid");
        optionsValidationMessage.classList.remove("d-none");
    };

    const clearOptionsValidationError = () => {
        optionsContainer.classList.remove("is-invalid");
        optionsValidationMessage.classList.add("d-none");
    };

    const serializeTag = (tag) => (tag.isNew
        ? {key: tag.key, value: tag.value}
        : tag.id);

    const syncTagsJson = () => {
        tagsJsonField.value = JSON.stringify(Array.from(selectedTags.values()).map(serializeTag));
    };

    const hideSuggestions = () => {
        tagSuggestions.classList.add("d-none");
        tagSuggestions.replaceChildren();
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

    const buildTagToken = (tag) => `${normalizeTagPart(tag.key)}::${normalizeTagPart(tag.value)}`;

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

    const selectTag = (tag) => {
        selectedTags.set(tag.id, tag);
        syncTagsJson();
        renderSelectedTags();
        tagSearchInput.value = "";
        hideSuggestions();
    };

    const renderSuggestions = (query) => {
        const normalizedQuery = query.trim().toLowerCase();
        if (!normalizedQuery) {
            hideSuggestions();
            return;
        }

        const matches = availableTags
            .filter((tag) => !hasSelectedTag(tag))
            .filter((tag) =>
                tag.key.toLowerCase().includes(normalizedQuery)
                || tag.value.toLowerCase().includes(normalizedQuery),
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

    const createOptionRow = (option = {}) => {
        const row = document.createElement("div");
        row.className = "option-row";
        row.innerHTML = `
            <input type="text" class="form-control option-label-input" placeholder="Label" value="${escapeHtml(option.label ?? "")}">
            <input type="text" class="form-control option-text-input" placeholder="Option text" value="${escapeHtml(option.text ?? "")}">
            <button type="button" class="btn btn-outline-danger option-remove-btn">Remove</button>
        `;

        row.querySelector(".option-remove-btn")?.addEventListener("click", () => {
            row.remove();
            syncOptionsJson();
        });

        row.querySelectorAll("input").forEach((input) => {
            input.addEventListener("input", () => {
                clearOptionsValidationError();
                syncOptionsJson();
            });
        });

        optionsContainer.appendChild(row);
    };

    const syncOptionsJson = () => {
        const options = Array.from(optionsContainer.querySelectorAll(".option-row"))
            .map((row) => ({
                label: row.querySelector(".option-label-input")?.value.trim() ?? "",
                text: row.querySelector(".option-text-input")?.value.trim() ?? "",
            }))
            .filter((option) => option.label || option.text);

        optionsJsonField.value = JSON.stringify({options});
        return options;
    };

    const initializeOptions = () => {
        try {
            const parsed = optionsJsonField.value.trim() ? JSON.parse(optionsJsonField.value) : null;
            const initialOptions = Array.isArray(parsed?.options) ? parsed.options : [];
            if (initialOptions.length > 0) {
                initialOptions.forEach((option) => createOptionRow(option));
                syncOptionsJson();
                return;
            }
        } catch (error) {
            console.error("Unable to parse initial options JSON", error);
        }

        createOptionRow();
        syncOptionsJson();
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
                    selectedTags.set(`new:${buildTagToken(entry)}`, {
                        id: `new:${buildTagToken(entry)}`,
                        key: entry.key.trim(),
                        value: entry.value.trim(),
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

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            await syncEditorValue();
        } catch (error) {
            console.error("Editor.js save failed", error);
            return;
        }

        const options = syncOptionsJson();
        const hasInvalidOption = options.length === 0 || options.some((option) => !option.label || !option.text);
        if (hasInvalidOption) {
            showOptionsValidationError();
            optionsContainer.scrollIntoView({behavior: "smooth", block: "center"});
            return;
        }

        if (!textarea.value.trim()) {
            showValidationError();
            holder.scrollIntoView({behavior: "smooth", block: "center"});
            return;
        }

        clearValidationError();
        clearOptionsValidationError();
        form.submit();
    });

    holder.addEventListener("input", clearValidationError);
    addOptionButton.addEventListener("click", () => {
        createOptionRow();
        clearOptionsValidationError();
    });
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
    document.addEventListener("click", (event) => {
        if (!tagAutocomplete.contains(event.target)) {
            hideSuggestions();
        }
    });
    initializeOptions();
    initializeTags();
})();
