function hideNonCitizenTokenActions() {
  document.querySelectorAll("#content tr").forEach((row) => {
    const role = row.querySelector("[data-role-select]")?.value;
    if (role && role !== "CITIZEN") {
      row
        .querySelectorAll("button[data-user]")
        .forEach((button) => button.remove());
    }
  });
  document.querySelectorAll("#content [data-role-select]").forEach((select) => {
    if (!select.dataset.currentRole) select.dataset.currentRole = select.value;
  });
  document
    .querySelectorAll("#user-token-history button[data-token]")
    .forEach((button) => button.closest("td")?.remove());
  document
    .querySelector("#user-token-history thead tr th:last-child")
    ?.remove();
}

new MutationObserver(hideNonCitizenTokenActions).observe(
  document.querySelector("#content"),
  { childList: true, subtree: true },
);

document.addEventListener(
  "click",
  (event) => {
    const button = event.target.closest("[data-user-role]");
    if (!button) return;
    const select = document.querySelector(
      `[data-role-select="${button.dataset.userRole}"]`,
    );
    if (select?.value !== select?.dataset.currentRole) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    notify(`This user is already assigned the ${select.value} role.`, true);
  },
  true,
);
