const form = document.querySelector("#create-user-form");
const message = document.querySelector("#form-message");
const passwordInput = form.elements.password;
const confirmPasswordInput = form.elements.confirmPassword;
function showMessage(text, error = false) {
  message.textContent = text;
  message.className = `notice${error ? " error" : ""}`;
}
function validatePasswordMatch() {
  const mismatch =
    confirmPasswordInput.value &&
    passwordInput.value !== confirmPasswordInput.value;
  confirmPasswordInput.setCustomValidity(
    mismatch ? "Password and confirm password do not match." : "",
  );
  return !mismatch;
}
function passwordMessage(password) {
  if (password.length < 8) return "Use at least 8 characters.";
  if (password.length > 72) return "Use no more than 72 characters.";
  if (/\s/.test(password)) return "Remove spaces from the password.";
  if (!/[a-z]/.test(password)) return "Add at least one lowercase letter.";
  if (!/[A-Z]/.test(password)) return "Add at least one uppercase letter.";
  if (!/\d/.test(password)) return "Add at least one number.";
  if (!/[^A-Za-z0-9\s]/.test(password))
    return "Add at least one symbol, such as !, @, or #.";
  return "";
}
form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form).entries());
  const passwordError = passwordMessage(data.password);
  if (passwordError) {
    showMessage(passwordError, true);
    return;
  }
  if (!validatePasswordMatch()) {
    showMessage(
      "Password and confirm password do not match. Please enter the same password in both fields.",
      true,
    );
    confirmPasswordInput.focus();
    return;
  }
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  button.textContent = "Creating account...";
  try {
    const response = await fetch("/api/v1/auth/register", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ email: data.email, password: data.password }),
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok || !payload?.success)
      throw new Error(
        payload?.error?.details?.password ||
          payload?.error?.details?.email ||
          payload?.error?.message ||
          payload?.message ||
          "We could not create your account.",
      );
    localStorage.setItem("smartqueue.auth", JSON.stringify(payload.data));
    window.location.assign("/");
  } catch (error) {
    showMessage(error.message, true);
    button.disabled = false;
    button.textContent = "Create account";
  }
});
passwordInput.addEventListener("input", validatePasswordMatch);
confirmPasswordInput.addEventListener("input", validatePasswordMatch);
