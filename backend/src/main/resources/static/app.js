const state = {
  auth: JSON.parse(localStorage.getItem("smartqueue.auth") || "null"),
  tab: "home",
  offices: [],
  departments: [],
  services: [],
  counters: [],
  selectedToken: null,
  selectedTokenLabel: "",
  selectedTokenStatus: null,
  selectedTokenServiceId: null,
  serviceNames: {},
  officerRefreshTimer: null,
  officerSocket: null,
  officerSocketServiceId: null,
  officerSocketRetry: null,
  liveSocket: null,
  liveSocketDestination: null,
  liveSocketRetry: null,
  liveSocketDebounce: null,
  liveSocketOnUpdate: null,
};
const $ = (selector) => document.querySelector(selector);
const content = $("#content");

function notify(message, error = false) {
  const node = $(state.auth ? "#notice" : "#auth-notice") || $("#notice");
  node.textContent = message;
  node.className = `notice${error ? " error" : ""}`;
  node.classList.remove("hidden");
  window.setTimeout(() => node.classList.add("hidden"), 5000);
}
function date() {
  return new Date().toISOString().slice(0, 10);
}
function esc(value) {
  return String(value ?? "").replace(
    /[&<>'"]/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[
        c
      ],
  );
}
function option(items, label = (x) => x.name || x.code || x.publicId) {
  return `<option value="">Select…</option>${items.map((x) => `<option value="${x.publicId}">${esc(label(x))}</option>`).join("")}`;
}
function status(value) {
  const cls = /CANCEL|NO_SHOW|CLOSED/.test(value)
    ? "bad"
    : /WAIT|SKIP/.test(value)
      ? "warn"
      : "";
  return `<span class="status ${cls}">${esc(value)}</span>`;
}
async function api(path, { method = "GET", body, download = false } = {}) {
  const headers = { Accept: "application/json" };
  if (body) headers["Content-Type"] = "application/json";
  if (state.auth?.accessToken)
    headers.Authorization = `Bearer ${state.auth.accessToken}`;
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: body && JSON.stringify(body),
  });
  if (response.status === 204) return null;
  if (download) {
    if (!response.ok) throw new Error(await response.text());
    return response.blob();
  }
  const payload = await response.json().catch(() => null);
  if (response.status === 401 && state.auth) {
    localStorage.removeItem("smartqueue.auth");
    state.auth = null;
    window.setTimeout(() => location.reload(), 50);
    throw new Error("Your session is no longer valid. Please sign in again.");
  }
  if (!response.ok || !payload?.success)
    throw new Error(
      payload?.error?.message ||
        payload?.message ||
        `Request failed (${response.status})`,
    );
  return payload.data;
}
async function download(path, filename) {
  const blob = await api(path, { download: true });
  const url = URL.createObjectURL(blob);
  const a = Object.assign(document.createElement("a"), {
    href: url,
    download: filename,
  });
  a.click();
  URL.revokeObjectURL(url);
}
function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}
function uuid() {
  const browserCrypto = window.crypto || window.msCrypto;
  if (browserCrypto?.randomUUID) return browserCrypto.randomUUID();

  const bytes = new Uint8Array(16);
  if (browserCrypto?.getRandomValues) {
    browserCrypto.getRandomValues(bytes);
  } else {
    for (let index = 0; index < bytes.length; index += 1)
      bytes[index] = Math.floor(Math.random() * 256);
  }

  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (byte) =>
    byte.toString(16).padStart(2, "0"),
  ).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
function authUserId() {
  if (state.auth?.userId) return state.auth.userId;
  try {
    return JSON.parse(
      atob(
        state.auth.accessToken
          .split(".")[1]
          .replace(/-/g, "+")
          .replace(/_/g, "/"),
      ),
    ).sub;
  } catch (_) {
    return null;
  }
}

function setup() {
  $("#login-form").onsubmit = async (e) => {
    e.preventDefault();
    await authenticate("/auth/login", formData(e.currentTarget));
  };
  $("#forgot-password")?.addEventListener("click", renderForgotPassword);
  $("#logout").onclick = () => {
    localStorage.removeItem("smartqueue.auth");
    state.auth = null;
    location.reload();
  };
  if (location.pathname === "/reset-password") {
    renderResetPassword();
    return;
  }
  if (state.auth) startApp();
}
function renderForgotPassword() {
  $("#auth-view").innerHTML =
    `<div id="auth-notice" class="notice notice-in-grid hidden" role="alert"></div><div><p class="eyebrow">Account recovery</p><h1>Reset your password.</h1><p class="muted">Enter the email address for your SmartQueue account and we will send a reset link.</p></div><form id="forgot-password-form"><h2>Forgot password?</h2><label>Email<input name="email" type="email" autocomplete="email" required></label><button>Send reset link</button><button type="button" class="auth-link" id="back-to-login">Back to sign in</button></form>`;
  $("#forgot-password-form").onsubmit = async (event) => {
    event.preventDefault();
    try {
      await api("/auth/forgot-password", {
        method: "POST",
        body: formData(event.currentTarget),
      });
      notify(
        "If an account matches that email, a password reset link has been sent.",
      );
    } catch (error) {
      notify(error.message, true);
    }
  };
  $("#back-to-login").onclick = () => (location.href = "/");
}
function renderResetPassword() {
  const token = new URLSearchParams(location.search).get("token");
  $("#auth-view").innerHTML =
    `<div id="auth-notice" class="notice notice-in-grid hidden" role="alert"></div><div><p class="eyebrow">Account recovery</p><h1>Choose a new password.</h1><p class="muted">Your new password must contain uppercase, lowercase, a number, and a symbol.</p></div><form id="reset-password-form"><h2>Reset password</h2><label>New password<input name="password" type="password" autocomplete="new-password" minlength="8" required></label><label>Confirm password<input name="confirmation" type="password" autocomplete="new-password" minlength="8" required></label><button>Set new password</button></form>`;
  $("#reset-password-form").onsubmit = async (event) => {
    event.preventDefault();
    const data = formData(event.currentTarget);
    if (!token) return notify("This password reset link is invalid.", true);
    if (data.password !== data.confirmation)
      return notify("Passwords do not match.", true);
    try {
      await api("/auth/reset-password", {
        method: "POST",
        body: { token, password: data.password },
      });
      notify("Password updated. You can now sign in.");
      setTimeout(() => (location.href = "/"), 1200);
    } catch (error) {
      notify(error.message, true);
    }
  };
}
async function authenticate(path, body) {
  try {
    state.auth = await api(path, { method: "POST", body });
    localStorage.setItem("smartqueue.auth", JSON.stringify(state.auth));
    startApp();
    notify("Signed in successfully.");
  } catch (e) {
    notify(e.message, true);
  }
}
function startApp() {
  $("#auth-view").classList.add("hidden");
  $("#app-view").classList.remove("hidden");
  $("#session").classList.remove("hidden");
  $("#identity").textContent = state.auth.email;
  renderNav();
  navigate("home");
  const citizenId = authUserId();
  if (state.auth.role === "CITIZEN" && citizenId)
    connectLiveSocket(
      `/topic/citizens/${citizenId}`,
      "citizen-queue",
      () => state.tab === "home" && citizenHome(),
    );
}
function renderNav() {
  const views =
    state.auth.role === "ADMIN"
      ? [
          ["home", "Dashboard"],
          ["users", "Users & tokens"],
          ["manage", "Manage data"],
          ["analytics", "Analytics"],
        ]
      : state.auth.role === "OFFICER"
        ? [
            ["home", "Counter desk"],
            ["operations", "Token operations"],
          ]
        : [
            ["home", "My queue"],
            ["book", "Book a token"],
            ["history", "History"],
          ];
  $("#nav").innerHTML = views
    .map(([id, name]) => `<button data-tab="${id}">${name}</button>`)
    .join("");
  $("#nav").onclick = (e) =>
    e.target.dataset.tab && navigate(e.target.dataset.tab);
}
async function navigate(tab) {
  if (tab !== "home") stopAppointmentTimers();
  clearOfficerDashboardTimer();
  clearLiveSocket();
  state.tab = tab;
  [...$("#nav").children].forEach((x) =>
    x.classList.toggle("active", x.dataset.tab === tab),
  );
  content.innerHTML = $("#loading").innerHTML;
  try {
    await (
      { home, book, history, operations, manage, analytics, users }[tab] || home
    )();
  } catch (e) {
    content.innerHTML = `<div class="card"><h2>Could not load this view</h2><p class="muted">${esc(e.message)}</p></div>`;
  }
}

async function loadCatalog() {
  state.offices = await api("/offices");
  return state.offices;
}
async function loadDepartments(officeId) {
  state.departments = officeId
    ? await api(`/departments?officeId=${officeId}`)
    : [];
  return state.departments;
}
async function loadServices(departmentId) {
  state.services = departmentId
    ? await api(`/services?departmentId=${departmentId}`)
    : [];
  return state.services;
}
async function loadServiceNames() {
  if (Object.keys(state.serviceNames).length) return;
  const offices = await api("/offices");
  for (const office of offices) {
    const departments = await api(`/departments?officeId=${office.publicId}`);
    for (const department of departments) {
      const services = await api(
        `/services?departmentId=${department.publicId}`,
      );
      services.forEach((service) => {
        state.serviceNames[service.publicId] = service.name;
      });
    }
  }
}
async function home() {
  if (state.auth.role === "ADMIN") return adminHome();
  if (state.auth.role === "OFFICER") return officerHome();
  return citizenHome();
}
async function live(event, target, serviceId, service) {
  event?.preventDefault();
  const selectedServiceId =
    serviceId || formData(event.currentTarget).serviceId;
  if (!selectedServiceId) return;
  const result = $(target);
  result.className = "loading";
  result.textContent = "Refreshing live queue...";
  try {
    const data = await api(
      `/queue/live-status?serviceId=${encodeURIComponent(selectedServiceId)}`,
    );
    const current = data.currentServing;
    result.className = "live-session";
    result.innerHTML = `<div class="live-session-summary"><div><span>Now serving</span><strong>${current ? `#${esc(current.tokenNumber)}` : "—"}</strong><small>${current ? esc(current.visitorName || "Visitor") : "No active token"}</small></div><div><span>Waiting</span><strong>${esc(data.waitingCount)}</strong><small>${data.waitingCount === 1 ? "person in queue" : "people in queue"}</small></div><div><span>Service</span><strong class="live-service-name">${esc(service?.name || current?.serviceName || "Selected service")}</strong><small>${service ? `${esc(service.startTime)}–${esc(service.endTime)}` : "Live status"}</small></div></div>${current ? `<div class="live-session-detail">Currently at <strong>${esc(current.counterCode || "an assigned counter")}</strong>${current.appointmentTime ? ` · appointment ${esc(current.appointmentTime)}` : ""}</div>` : '<p class="muted live-session-detail">No token is currently being served. The queue is ready for the next visitor.</p>'}`;
  } catch (e) {
    result.className = "empty";
    result.textContent = "Unable to load the live session.";
    notify(e.message, true);
  }
}
function timeSlots(service, appointmentDate = date()) {
  const slots = [];
  const [hour, minute] = service.startTime.split(":").map(Number);
  const [endHour, endMinute] = service.endTime.split(":").map(Number);
  const breakStart = service.breakStartTime
    ? service.breakStartTime.split(":").map(Number)
    : null;
  const breakEnd = service.breakEndTime
    ? service.breakEndTime.split(":").map(Number)
    : null;
  const breakStartMinutes = breakStart
    ? breakStart[0] * 60 + breakStart[1]
    : null;
  const breakEndMinutes = breakEnd ? breakEnd[0] * 60 + breakEnd[1] : null;
  const now = new Date();
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const isToday = appointmentDate === date();
  let current = hour * 60 + minute;
  const end = endHour * 60 + endMinute;
  while (current < end) {
    const duringBreak =
      breakStartMinutes !== null &&
      current >= breakStartMinutes &&
      current < breakEndMinutes;
    if ((!isToday || current > nowMinutes) && !duringBreak)
      slots.push(
        `${String(Math.floor(current / 60)).padStart(2, "0")}:${String(current % 60).padStart(2, "0")}`,
      );
    current += service.averageServiceMinutes;
  }
  return slots;
}
const weekdayNames = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];
const weekdayDisplayOrder = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
function orderedOpenDays(service) {
  const openDays = new Set(service.openDays || weekdayNames);
  return weekdayDisplayOrder.filter((day) => openDays.has(day));
}
function isServiceOpenOn(service, isoDate) {
  return (service.openDays || weekdayNames).includes(
    weekdayNames[new Date(`${isoDate}T00:00:00`).getDay()],
  );
}
function nextOpenServiceDate(service, fromDate) {
  const candidate = new Date(`${fromDate}T00:00:00`);
  for (let i = 0; i <= 30; i += 1) {
    const iso = `${candidate.getFullYear()}-${String(candidate.getMonth() + 1).padStart(2, "0")}-${String(candidate.getDate()).padStart(2, "0")}`;
    if (isServiceOpenOn(service, iso)) return iso;
    candidate.setDate(candidate.getDate() + 1);
  }
  return null;
}
function nextBookableServiceDate(service, fromDate) {
  const candidate = new Date(`${fromDate}T00:00:00`);
  for (let i = 0; i <= 30; i += 1) {
    const iso = `${candidate.getFullYear()}-${String(candidate.getMonth() + 1).padStart(2, "0")}-${String(candidate.getDate()).padStart(2, "0")}`;
    const serviceHasEndedToday =
      iso === date() && new Date() >= new Date(`${iso}T${service.endTime}`);
    if (isServiceOpenOn(service, iso) && !serviceHasEndedToday) return iso;
    candidate.setDate(candidate.getDate() + 1);
  }
  return null;
}
async function book() {
  await loadCatalog();
  const today = date();
  const maxDate = new Date(Date.now() + 30 * 86400000)
    .toISOString()
    .slice(0, 10);
  content.innerHTML = `<section class="card"><p class="eyebrow">Book a visit</p><h2>Choose your appointment</h2><form id="book-form" class="form-grid"><label>Full name<input name="visitorName" autocomplete="name" maxlength="150" required></label><label>Phone number<input name="visitorPhone" type="tel" autocomplete="tel" maxlength="30" placeholder="e.g. +91 98765 43210" required></label><label>Service type<select id="office-category" required><option value="">Select hospital or RTO...</option><option value="HOSPITAL">Hospital</option><option value="RTO">RTO</option></select></label><label>Hospital or RTO location<select id="office" disabled required><option>Select service type first...</option></select></label><label>Department<select id="department" disabled required><option>Select location first...</option></select></label><label>Service<select name="serviceId" id="service" disabled required><option>Select department first...</option></select></label><p id="slot-interval" class="full muted">Select a service to view its appointment interval.</p><label>Date<input name="appointmentDate" type="date" min="${today}" max="${maxDate}" value="${today}" required></label><label>Time slot<select name="appointmentTime" id="appointment-time" disabled required><option>Select a service first...</option></select></label><button class="full">Book appointment</button></form></section>`;
  $("#book-form label:nth-of-type(2)").insertAdjacentHTML(
    "afterend",
    `<label>Age<input name="visitorAge" type="number" min="0" max="130" autocomplete="off" required></label><label>Gender<select name="visitorGender" required><option value="">Select gender...</option><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option><option value="PREFER_NOT_TO_SAY">Prefer not to say</option></select></label>`,
  );
  const resetAfterType = () => {
    $("#department").innerHTML = "<option>Select location first...</option>";
    $("#department").disabled = true;
    $("#service").innerHTML = "<option>Select department first...</option>";
    $("#service").disabled = true;
    $("#appointment-time").innerHTML =
      "<option>Select a service first...</option>";
    $("#appointment-time").disabled = true;
    $("#slot-interval").textContent =
      "Select a service to view its appointment interval.";
  };
  $("#office-category").onchange = (event) => {
    const offices = state.offices.filter(
      (office) => office.category === event.target.value,
    );
    $("#office").innerHTML = offices.length
      ? `<option value="">Select a location...</option>${offices.map((office) => `<option value="${office.publicId}">${esc(office.name)} — ${esc(office.address)}</option>`).join("")}`
      : "<option>No locations available</option>";
    $("#office").disabled = !offices.length;
    resetAfterType();
  };
  $("#office").onchange = async (event) => {
    await loadDepartments(event.target.value);
    $("#department").innerHTML = option(state.departments);
    $("#department").disabled = false;
    $("#service").innerHTML = "<option>Select department first...</option>";
    $("#service").disabled = true;
    $("#appointment-time").innerHTML =
      "<option>Select a service first...</option>";
    $("#appointment-time").disabled = true;
    $("#slot-interval").textContent =
      "Select a service to view its appointment interval.";
  };
  $("#department").onchange = async (event) => {
    await loadServices(event.target.value);
    $("#service").innerHTML = option(
      state.services,
      (service) => `${service.name} · ${service.startTime}-${service.endTime}`,
    );
    $("#service").disabled = false;
  };
  const refreshSlots = async () => {
    const service = state.services.find(
      (item) => item.publicId === $("#service").value,
    );
    const appointmentDate = $('#book-form [name="appointmentDate"]').value;
    if (!service || !appointmentDate) return;
    $("#appointment-time").innerHTML =
      '<option value="">Loading available slots...</option>';
    $("#appointment-time").disabled = true;
    try {
      const slots = await api(
        `/tokens/available-slots?serviceId=${encodeURIComponent(service.publicId)}&appointmentDate=${encodeURIComponent(appointmentDate)}`,
      );
      $("#appointment-time").innerHTML = slots.length
        ? `<option value="">Select a time slot...</option>${slots.map((slot) => `<option value="${slot}">${slot}</option>`).join("")}`
        : '<option value="">No available slots for this date</option>';
      $("#appointment-time").disabled = !slots.length;
      $("#slot-interval").textContent = `Open ${orderedOpenDays(service)
        .map((day) => day[0] + day.slice(1).toLowerCase())
        .join(
          ", ",
        )}. Showing available appointments every ${service.averageServiceMinutes} minutes.`;
    } catch (error) {
      $("#appointment-time").innerHTML =
        '<option value="">Could not load available slots</option>';
      notify(error.message, true);
    }
  };
  $("#service").onchange = () => {
    const service = state.services.find(
      (item) => item.publicId === $("#service").value,
    );
    const dateInput = $('#book-form [name="appointmentDate"]');
    const nextDate =
      service && nextBookableServiceDate(service, dateInput.value);
    if (nextDate && nextDate !== dateInput.value) {
      dateInput.value = nextDate;
      notify(
        `This service is unavailable on the selected day. Showing the next available date: ${nextDate}.`,
      );
    }
    refreshSlots();
  };
  $('#book-form [name="appointmentDate"]').onchange = () => {
    const service = state.services.find(
      (item) => item.publicId === $("#service").value,
    );
    const dateInput = $('#book-form [name="appointmentDate"]');
    if (
      service &&
      nextBookableServiceDate(service, dateInput.value) !== dateInput.value
    ) {
      const nextDate = nextBookableServiceDate(service, dateInput.value);
      if (nextDate) {
        dateInput.value = nextDate;
        notify(
          `This service is unavailable on that day. Showing the next available date: ${nextDate}.`,
        );
      }
    }
    refreshSlots();
  };
  $("#book-form").onsubmit = async (event) => {
    event.preventDefault();
    try {
      await api("/tokens", {
        method: "POST",
        body: { ...formData(event.currentTarget), idempotencyKey: uuid() },
      });
      notify("Appointment booked successfully.");
      navigate("home");
    } catch (error) {
      notify(error.message, true);
    }
  };
}

// Minimal native STOMP client: no external browser library is required.
connectOfficerQueueSocket = function (serviceId, refresh) {
  if (
    state.officerSocketServiceId === serviceId &&
    state.officerSocket?.readyState === WebSocket.OPEN
  )
    return;
  if (state.officerSocket) {
    state.officerSocket.onclose = null;
    state.officerSocket.close();
  }
  state.officerSocketServiceId = serviceId;
  const connect = () => {
    if (state.tab !== "home" || state.officerSocketServiceId !== serviceId)
      return;
    const socket = new WebSocket(
      `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`,
    );
    state.officerSocket = socket;
    socket.onopen = () =>
      socket.send(
        `CONNECT\naccept-version:1.2\nAuthorization:Bearer ${state.auth.accessToken}\n\n\0`,
      );
    socket.onmessage = (event) => {
      const frame = String(event.data);
      if (frame.startsWith("CONNECTED")) {
        socket.send(
          `SUBSCRIBE\nid:officer-service-${serviceId}\ndestination:/topic/services/${serviceId}\n\n\0`,
        );
        return;
      }
      if (!frame.startsWith("MESSAGE")) return;
      try {
        const payload = JSON.parse(
          frame.slice(frame.indexOf("\n\n") + 2).replace(/\0$/, ""),
        );
        if (payload.serviceId === serviceId) refresh();
      } catch (_) {}
    };
    socket.onerror = () => socket.close();
    socket.onclose = () => {
      if (state.tab === "home" && state.officerSocketServiceId === serviceId)
        state.officerSocketRetry = setTimeout(connect, 2000);
    };
  };
  connect();
};

// The officer view is intentionally defined last: it owns the live WebSocket subscription.
async function liveOfficerHome() {
  const counter = (await api("/officer/counters"))[0];
  if (!counter) {
    content.innerHTML =
      '<section class="card"><h2>No assigned counter</h2><p class="empty">Contact an administrator for an active counter assignment.</p></section>';
    return;
  }
  const statuses = [
    "WAITING",
    "CALLED",
    "SKIPPED",
    "COMPLETED",
    "CANCELLED",
    "NO_SHOW",
  ];
  content.innerHTML = `<section class="card"><p class="eyebrow">Officer dashboard</p><h2>Live queue for your counter</h2><div class="grid officer-overview"><div class="span-4">Assigned counter<strong>${esc(counter.counterCode)}</strong></div><div class="span-4">Assigned service<strong id="assigned-service"></strong></div><div class="span-4">Status<strong id="counter-status">Loading…</strong></div></div></section><section class="card"><form id="officer-filters" class="toolbar"><label>Date<input type="date" name="date" value="${date()}"></label><label>Service<select name="serviceId">${counter.services.map((service) => `<option value="${service.serviceId}">${esc(service.serviceName)}</option>`).join("")}</select></label><fieldset class="status-filter"><legend>Status</legend>${statuses.map((value) => `<label><input type="checkbox" name="statuses" value="${value}">${value.replace("_", " ")}</label>`).join("")}</fieldset><label>Arrival<select name="arrived"><option value="">All</option><option value="true">Arrived</option><option value="false">Not arrived</option></select></label></form></section><div id="officer-dashboard" class="loading">Loading live queue…</div><section id="officer-user-panel" class="card hidden"></section>`;
  const form = $("#officer-filters");
  const showUser = (token) => {
    const panel = $("#officer-user-panel");
    panel.innerHTML = `<div class="actions"><h2>User details — token #${token.tokenNumber}</h2><button class="secondary" id="close-user-detail">Close</button></div>${userDetail(token)}`;
    panel.classList.remove("hidden");
    panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
    $("#close-user-detail").onclick = () => panel.classList.add("hidden");
  };
  const load = async () => {
    const values = new FormData(form);
    const query = new URLSearchParams({
      date: values.get("date"),
      serviceId: values.get("serviceId"),
    });
    values
      .getAll("statuses")
      .forEach((value) => query.append("statuses", value));
    if (values.get("arrived")) query.set("arrived", values.get("arrived"));
    try {
      const dashboard = await api(
        `/officer/counters/${counter.counterId}/dashboard?${query}`,
      );
      connectOfficerQueueSocket(values.get("serviceId"), load);
      const waiting = dashboard.tokens.filter((token) =>
        ["WAITING", "SKIPPED"].includes(token.status),
      );
      $("#assigned-service").textContent =
        counter.services.map((service) => service.serviceName).join(", ") ||
        dashboard.serviceName;
      $("#counter-status").innerHTML = status(dashboard.counterStatus);
      $("#officer-dashboard").innerHTML =
        `<div class="grid"><section class="card span-5"><p class="eyebrow">Current token</p>${dashboard.currentToken ? `<h2>Now serving #${dashboard.currentToken.tokenNumber}</h2><p>${status(dashboard.currentToken.status)} · ${esc(dashboard.currentToken.visitorName)}</p><button class="secondary" data-user-detail="${dashboard.currentToken.publicId}">View user details</button>` : '<h2>No token is being served</h2><p class="muted">Call the next token from Token operations.</p>'}</section><section class="card span-7"><p class="eyebrow">Today's statistics</p><div class="stats-row"><div>Completed<strong>${dashboard.completedCount}</strong></div><div>Cancelled<strong>${dashboard.cancelledCount}</strong></div><div>Average wait<strong>${dashboard.averageWaitMinutes} min</strong></div></div><p class="muted">${dashboard.arrivedCount} arrived · ${dashboard.waitingCount} waiting</p></section><section class="card span-12"><p class="eyebrow">Waiting queue</p><h2>${esc(dashboard.queueDate)} live queue</h2>${officerRows(waiting)}</section><section class="card span-12"><p class="eyebrow">Filtered tokens</p><h2>All matching tokens</h2>${officerRows(dashboard.tokens)}</section></div>`;
      $("#officer-dashboard").onclick = (event) => {
        const detail = event.target.closest("[data-user-detail]");
        const manage = event.target.closest("[data-token]");
        if (detail) {
          const token =
            dashboard.tokens.find(
              (item) => item.publicId === detail.dataset.userDetail,
            ) ||
            (dashboard.currentToken?.publicId === detail.dataset.userDetail
              ? dashboard.currentToken
              : null);
          if (token) showUser(token);
        }
        if (manage) {
          state.selectedToken = manage.dataset.token;
          state.selectedTokenLabel = manage.dataset.tokenLabel;
          navigate("operations");
        }
      };
    } catch (error) {
      $("#officer-dashboard").innerHTML =
        `<section class="card"><p class="empty">${esc(error.message)}</p></section>`;
    }
  };
  form.onchange = () => {
    clearOfficerDashboardTimer();
    load();
  };
  await load();
  state.officerRefreshTimer = setInterval(
    () => state.tab === "home" && load(),
    15000,
  );
}

// Officers may change only the status of their active assignment; counter creation/deletion remains admin-only.
const renderOfficerDashboard = liveOfficerHome;
officerHome = async function () {
  await renderOfficerDashboard();
  const assigned = await api("/officer/counters");
  const counter = assigned[0];
  const statusNode = $("#counter-status");
  if (!counter || !statusNode) return;
  const isOpen = statusNode.textContent.trim() === "OPEN";
  statusNode.parentElement.insertAdjacentHTML(
    "beforeend",
    `<button class="secondary" id="counter-status-toggle" data-next-status="${isOpen ? "close" : "open"}">${isOpen ? "Close counter" : "Open counter"}</button>`,
  );
  $("#counter-status-toggle").onclick = async (event) => {
    const action = event.currentTarget.dataset.nextStatus;
    try {
      await api(`/officer/counters/${counter.counterId}/${action}`, {
        method: "POST",
      });
      notify(`Counter ${action === "open" ? "opened" : "closed"}.`);
      navigate("home");
    } catch (error) {
      notify(error.message, true);
    }
  };
};

const renderUsers = users;
users = async (...args) => {
  await renderUsers(...args);
  content.querySelectorAll("tr").forEach((row) => {
    const role = row.querySelector("[data-role-select]")?.value;
    if (role === "OFFICER" || role === "ADMIN") {
      row
        .querySelectorAll("button[data-user]")
        .forEach((button) => button.remove());
    }
  });
};

document.addEventListener(
  "click",
  async (event) => {
    const button = event.target.closest("#content button[data-user]");
    if (!button) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    try {
      const history = await api(
        `/users/${button.dataset.user}/tokens?page=0&size=100`,
      );
      const panel = $("#user-token-history");
      const email =
        button.closest("tr")?.querySelector("td")?.textContent?.trim() ||
        "User";
      panel.classList.remove("hidden");
      panel.innerHTML = `<p class="eyebrow">User token history</p><h2>${esc(email)} token history</h2>${tokenTable(history.content)}`;
    } catch (error) {
      notify(error.message, true);
    }
  },
  true,
);

async function history() {
  await loadServiceNames();
  const page = await api("/tokens/history?page=0&size=100");
  const appointments = [...page.content]
    .sort((a, b) => a.tokenNumber - b.tokenNumber)
    .map((appointment, index) => ({ ...appointment, tokenNumber: index + 1 }));
  content.innerHTML = `<section class="card"><h2>Appointment history</h2>${tokenTable(appointments)}</section>`;
}

function formattedCounterOptions(counters) {
  const groups = new Map();
  counters.forEach((counter) => {
    const type =
      counter.officeCategory === "RTO"
        ? "RTO"
        : counter.officeCategory === "HOSPITAL"
          ? "Hospital"
          : "Other";
    const location = counter.officeName || "Unspecified location";
    const label = `${type} location: ${location}`;
    if (!groups.has(label)) groups.set(label, []);
    groups.get(label).push(counter);
  });
  return [...groups.entries()]
    .map(
      ([label, items]) =>
        `<optgroup label="${esc(label)}">${items.map((counter) => `<option value="${counter.counterId}">${esc(counter.counterCode)}</option>`).join("")}</optgroup>`,
    )
    .join("");
}
function counterOptions(counters) {
  return formattedCounterOptions(counters);
}
function operationActions(status) {
  const choices =
    status === "CALLED"
      ? [
          ["complete", "Complete"],
          ["skip", "Skip"],
          ["no-show", "Mark no-show"],
        ]
      : status === "SKIPPED"
        ? [
            ["recall", "Recall"],
            ["no-show", "Mark no-show"],
          ]
        : [["no-show", "Mark no-show"]];
  return `<label>Action<select name="action">${choices.map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}</select></label>`;
}

async function users(activity = "ALL", userEmail = "", userState = "ALL") {
  await loadServiceNames();
  const [people, tokenPage] = await Promise.all([
    api("/users"),
    api(`/users/tokens?activity=${activity}&page=0&size=100`),
  ]);
  const normalizedEmail = userEmail.trim().toLowerCase();
  const displayedPeople = people.filter(
    (person) =>
      (!normalizedEmail ||
        person.email.toLowerCase().includes(normalizedEmail)) &&
      (userState === "ALL" || (userState === "ACTIVE") === person.enabled),
  );
  const canManageUsers = state.auth.role === "ADMIN";
  const roleOptions = (role) =>
    ["CITIZEN", "OFFICER", "ADMIN"]
      .map(
        (value) =>
          `<option value="${value}"${value === role ? " selected" : ""}>${value}</option>`,
      )
      .join("");
  content.innerHTML = `<section class="card"><p class="eyebrow">User directory</p><h2>Users</h2><form id="user-filter-form" class="toolbar"><label>Filter users by email<input name="email" type="search" value="${esc(userEmail)}" placeholder="Email address"></label><label>Filter users by state<select name="state"><option value="ALL"${userState === "ALL" ? " selected" : ""}>All users</option><option value="ACTIVE"${userState === "ACTIVE" ? " selected" : ""}>Active users</option><option value="DISABLED"${userState === "DISABLED" ? " selected" : ""}>Disabled users</option></select></label><button type="submit">Apply filters</button><button class="secondary" type="button" id="clear-user-filters">Clear filters</button></form><div class="table-wrap"><table><thead><tr><th>Email</th><th>Role</th><th>State</th><th>Actions</th></tr></thead><tbody>${displayedPeople
    .map((person) => {
      const isCurrentUser = person.email === state.auth.email;
      const canViewTokens = person.role !== "ADMIN";
      return `<tr><td>${esc(person.email)}</td><td>${canManageUsers && !isCurrentUser ? `<select aria-label="Role for ${esc(person.email)}" data-role-select="${person.publicId}">${roleOptions(person.role)}</select>` : esc(person.role)}</td><td>${person.enabled ? status("ACTIVE") : status("DISABLED")}</td><td>${canViewTokens ? `<button data-user="${person.publicId}">View tokens</button>` : ""}${canManageUsers && !isCurrentUser ? `<button class="secondary" data-user-role="${person.publicId}">Update role</button>${person.role !== "ADMIN" ? `<button class="danger" data-user-delete="${person.publicId}">Delete user</button>` : ""}` : ""}</td></tr>`;
    })
    .join(
      "",
    )}</tbody></table></div></section><section id="all-token-history" class="card"><p class="eyebrow">Token directory</p><h2>All token history</h2><form id="token-filter-form" class="toolbar"><label>Filter tokens<select name="activity"><option value="ALL"${activity === "ALL" ? " selected" : ""}>All tokens</option><option value="ACTIVE"${activity === "ACTIVE" ? " selected" : ""}>Active tokens</option><option value="INACTIVE"${activity === "INACTIVE" ? " selected" : ""}>Inactive tokens</option></select></label><button type="submit">Apply filter</button><button class="secondary" type="button" id="clear-token-filters">Clear filter</button></form>${tokenTable(tokenPage.content)}</section><section id="user-token-history" class="card hidden"></section>`;
  $("#user-filter-form").onsubmit = (event) => {
    event.preventDefault();
    const filter = formData(event.currentTarget);
    users(activity, filter.email, filter.state);
  };
  $("#clear-user-filters").onclick = () => users(activity);
  $("#token-filter-form").onsubmit = (event) => {
    event.preventDefault();
    users(formData(event.currentTarget).activity, userEmail, userState);
  };
  $("#clear-token-filters").onclick = () => users("ALL", userEmail, userState);
  content.onclick = async (event) => {
    const roleButton = event.target.closest("[data-user-role]");
    if (roleButton) {
      const userId = roleButton.dataset.userRole;
      const role = content.querySelector(
        `[data-role-select="${userId}"]`,
      ).value;
      if (confirm(`Change this user's role to ${role}?`))
        await mutation(
          `/users/${userId}/role`,
          { role },
          () => users(activity, userEmail, userState),
          "PUT",
        );
      return;
    }
    const deleteButton = event.target.closest("[data-user-delete]");
    if (deleteButton) {
      const userId = deleteButton.dataset.userDelete;
      if (
        confirm(
          "Are you sure you want to permanently delete this user account?",
        )
      ) {
        await mutation(
          `/users/${userId}`,
          {},
          () => users(activity, userEmail, userState),
          "DELETE",
        );
        notify("User deleted successfully.");
      }
      return;
    }
    const userButton = event.target.closest("[data-user]");
    if (!userButton) return;
    try {
      const history = await api(
        `/users/${userButton.dataset.user}/tokens?page=0&size=100`,
      );
      const panel = $("#user-token-history");
      panel.classList.remove("hidden");
      panel.innerHTML = `<p class="eyebrow">User token history</p><h2>${esc(people.find((person) => person.publicId === userButton.dataset.user)?.email || "User")} token history</h2>${tokenTable(history.content, true)}`;
    } catch (error) {
      notify(error.message, true);
    }
  };
}
async function refreshAdminDashboardMetrics() {
  try {
    const dashboard = await api("/analytics/dashboard");
    document.querySelectorAll(".metric strong").forEach((node, index) => {
      const value = Object.values(dashboard)[index];
      if (value !== undefined) node.textContent = value;
    });
  } catch (_) {}
}
async function adminHome() {
  const [dashboard, offices] = await Promise.all([
    api("/analytics/dashboard"),
    api("/offices").catch(() => []),
  ]);
  state.offices = offices;
  content.innerHTML = `<section><p class="eyebrow">Administrator dashboard</p><div class="grid">${Object.entries(
    dashboard,
  )
    .map(
      ([key, value]) =>
        `<div class="card metric span-3"><span>${key.replace(/([A-Z])/g, " $1")}</span><strong>${esc(value)}</strong></div>`,
    )
    .join(
      "",
    )}</div></section><section class="card"><div class="section-heading"><div><p class="eyebrow">Live session</p><h2>Live queue monitor</h2><p class="muted">Choose a location and service to see its current queue.</p></div><button class="secondary" type="button" id="live-refresh" disabled>Refresh</button></div><form id="live-form" class="form-grid live-filter"><label>Location<select name="officeId" id="live-office"><option value="">Select a location...</option>${offices.map((office) => `<option value="${office.publicId}">${esc(office.name)}</option>`).join("")}</select></label><label>Department<select name="departmentId" id="live-department" disabled><option value="">Select a location first...</option></select></label><label class="full">Service<select name="serviceId" id="live-service" disabled required><option value="">Select a department first...</option></select></label></form><div id="live-result" class="empty">Select a service to view its live session.</div></section>`;
  const office = $("#live-office"),
    department = $("#live-department"),
    service = $("#live-service"),
    refresh = $("#live-refresh");
  office.onchange = async () => {
    department.disabled = true;
    service.disabled = true;
    refresh.disabled = true;
    department.innerHTML = '<option value="">Loading departments...</option>';
    service.innerHTML =
      '<option value="">Select a department first...</option>';
    $("#live-result").className = "empty";
    $("#live-result").textContent =
      "Select a department and service to view its live session.";
    try {
      const departments = await loadDepartments(office.value);
      department.innerHTML = departments.length
        ? `<option value="">Select a department...</option>${departments.map((item) => `<option value="${item.publicId}">${esc(item.name)}</option>`).join("")}`
        : '<option value="">No departments available</option>';
      department.disabled = !departments.length;
    } catch (error) {
      department.innerHTML =
        '<option value="">Could not load departments</option>';
      notify(error.message, true);
    }
  };
  department.onchange = async () => {
    service.disabled = true;
    refresh.disabled = true;
    service.innerHTML = '<option value="">Loading services...</option>';
    $("#live-result").className = "empty";
    $("#live-result").textContent =
      "Select a service to view its live session.";
    try {
      const services = await loadServices(department.value);
      service.innerHTML = services.length
        ? `<option value="">Select a service...</option>${services.map((item) => `<option value="${item.publicId}">${esc(item.name)} · ${esc(item.startTime)}–${esc(item.endTime)}</option>`).join("")}`
        : '<option value="">No services available</option>';
      service.disabled = !services.length;
    } catch (error) {
      service.innerHTML = '<option value="">Could not load services</option>';
      notify(error.message, true);
    }
  };
  const updateLive = () =>
    live(
      null,
      "#live-result",
      service.value,
      state.services.find((item) => item.publicId === service.value),
    );
  connectLiveSocket(
    "/topic/admin/queue",
    "admin-queue",
    () => state.tab === "home" && refreshAdminDashboardMetrics(),
  );
  service.onchange = () => {
    refresh.disabled = !service.value;
    if (service.value) {
      connectLiveSocket("/topic/admin/queue", "admin-queue", () => {
        if (state.tab === "home") {
          refreshAdminDashboardMetrics();
          updateLive();
        }
      });
      updateLive();
    }
  };
  refresh.onclick = updateLive;
}
async function manage() {
  await loadCatalog();
  content.innerHTML = `<div class="grid"><section class="card span-4"><h2>Offices</h2>${officeForm()}${table(state.offices, ["code", "name"], "offices")}</section><section class="card span-4"><h2>Departments</h2>${departmentForm()}<div id="department-list" class="empty">Choose an office to list departments.</div></section><section class="card span-4"><h2>Services</h2>${serviceForm()}<div id="service-list" class="empty">Choose a department to list services.</div></section><section class="card span-12"><h2>Counters & assignments</h2>${counterForm()}<div id="counter-list" class="empty">Choose an office to list counters.</div></section></div>`;
  bindManagement();
}
function officeForm() {
  return `<form id="office-form"><div class="form-grid"><label>Service type<select name="category" required><option value="HOSPITAL">Hospital</option><option value="RTO">RTO</option><option value="OTHER">Other</option></select></label><label>Location code<input name="code" required maxlength="30" placeholder="e.g. HOSPITAL-01"></label><label>Location name<input name="name" required maxlength="150" placeholder="e.g. Gangaram Hospital"></label><label>Location / address<input name="address" required maxlength="500"></label><button class="full">Add location</button></div></form>`;
}
function departmentForm() {
  return `<form id="department-form"><label>Office<select name="officeId" id="department-office" required>${option(state.offices, (x) => x.name)}</select></label><label>Name<input name="name" required></label><button>Add department</button></form>`;
}
function serviceForm() {
  const days = [
    ["MONDAY", "Mon"],
    ["TUESDAY", "Tue"],
    ["WEDNESDAY", "Wed"],
    ["THURSDAY", "Thu"],
    ["FRIDAY", "Fri"],
    ["SATURDAY", "Sat"],
    ["SUNDAY", "Sun"],
  ];
  return `<form id="service-form" class="form-grid"><label class="full">Department<select name="departmentId" id="service-department" required><option>Select an office first…</option></select></label><label>Name<input name="name" required></label><label>Capacity<input name="dailyCapacity" type="number" min="1" required></label><label>Start<input name="startTime" type="time" required></label><label>End<input name="endTime" type="time" required></label><label>Break start <input name="breakStartTime" type="time"><small>Optional. Set both break times to block bookings during the break.</small></label><label>Break end <input name="breakEndTime" type="time"></label><fieldset class="full weekday-picker"><legend>Open days</legend><p class="muted">Customers can book this service only on the selected days.</p><div>${days.map(([value, label]) => `<label><input type="checkbox" name="openDays" value="${value}" checked>${label}</label>`).join("")}</div></fieldset><label class="full">Approximate service time / slot interval (minutes)<input name="averageServiceMinutes" type="number" min="5" step="5" readonly><small>Calculated as (end time − start time) ÷ capacity, rounded to the nearest 5 minutes.</small></label><button class="full">Add service</button></form>`;
}
function table(items, fields, type) {
  return `<div class="table-wrap"><table><tbody>${items.map((x) => `<tr>${fields.map((f) => `<td>${esc(x[f])}</td>`).join("")}<td><button class="danger" data-delete="${type}" data-id="${x.publicId}">Delete</button></td></tr>`).join("")}</tbody></table></div>`;
}
function counterForm() {
  return `<form id="counter-form" class="form-grid"><label>Office<select name="officeId" id="counter-office" required>${option(state.offices, (x) => x.name)}</select></label><label>Counter code<input name="code" required maxlength="30" placeholder="e.g. DL-01 or OPD-01"></label><button>Add counter</button></form><p class="muted">Select an office to edit codes, open or close desks, and manage service and officer assignments.</p>`;
}
function bindManagement() {
  $("#office-form").onsubmit = (e) => adminPost(e, "/offices", manage);
  $("#department-form").onsubmit = (e) => adminPost(e, "/departments", manage);
  $("#service-form").onsubmit = (e) => adminPost(e, "/services", manage);
  $("#counter-form").onsubmit = (e) => adminPost(e, "/counters", manage);
  const refresh = () => {
    const form = $("#service-form"),
      start = form.startTime.value,
      end = form.endTime.value,
      capacity = Number(form.dailyCapacity.value);
    if (!start || !end || !capacity) return;
    const [sh, sm] = start.split(":").map(Number),
      [eh, em] = end.split(":").map(Number);
    const duration = eh * 60 + em - sh * 60 - sm;
    form.averageServiceMinutes.value =
      duration > 0 ? Math.max(5, Math.round(duration / capacity / 5) * 5) : "";
  };
  ["startTime", "endTime", "dailyCapacity"].forEach((name) =>
    $("#service-form").elements[name].addEventListener("input", refresh),
  );
  $("#department-office").onchange = async (e) => {
    await loadDepartments(e.target.value);
    $("#department-list").innerHTML = table(
      state.departments,
      ["name"],
      "departments",
    );
    $("#service-department").innerHTML = option(
      state.departments,
      (x) => x.name,
    );
  };
  $("#service-department").onchange = async (e) => {
    await loadServices(e.target.value);
    $("#service-list").innerHTML = table(
      state.services,
      ["name", "dailyCapacity"],
      "services",
    );
  };
  $("#counter-office").onchange = (e) => loadCounterManagement(e.target.value);
  content.onclick = (e) => {
    const b = e.target.closest("[data-delete]");
    if (b && confirm("Deactivate this record?"))
      mutation(`/${b.dataset.delete}/${b.dataset.id}`, {}, {}, "DELETE").then(
        manage,
      );
  };
}
function counterManagementCard(
  counter,
  officeId,
  officers,
  services,
  departmentNames,
) {
  const assigned = new Set(counter.services.map((s) => s.publicId)),
    available = services.filter((s) => !assigned.has(s.publicId));
  return `<section class="card"><div class="actions"><h3 class="action-title">${esc(counter.code)} ${status(counter.status)}</h3><button class="secondary" data-counter-edit="${counter.publicId}" data-code="${esc(counter.code)}">Edit</button><button data-counter-status="${counter.publicId}" data-next-status="${counter.status === "OPEN" ? "close" : "open"}">${counter.status === "OPEN" ? "Close" : "Open"}</button><button class="danger" data-counter-deactivate="${counter.publicId}" ${counter.status === "OPEN" ? "disabled" : ""}>Deactivate</button></div><p><strong>Officer:</strong> ${counter.officer ? esc(counter.officer.email) : "Unassigned"} ${counter.officer ? `<button class="link-danger" data-officer-release="${counter.publicId}">Release</button>` : ""}</p>${!counter.officer && officers.length ? `<form data-officer-assignment="${counter.publicId}" class="toolbar"><select name="officerId">${officers.map((o) => `<option value="${o.publicId}">${esc(o.email)}</option>`).join("")}</select><button>Assign officer</button></form>` : ""}<p><strong>Services:</strong> ${counter.services.length ? counter.services.map((s) => `${esc(s.name)} <button class="link-danger" data-service-release="${counter.publicId}" data-service-id="${s.publicId}">Remove</button>`).join(" · ") : "None assigned"}</p>${available.length ? `<form data-service-assignment="${counter.publicId}" class="toolbar"><select name="serviceId">${available.map((s) => `<option value="${s.publicId}">${esc(s.name)} — ${esc(departmentNames.get(s.departmentId))}</option>`).join("")}</select><button>Assign service</button></form>` : ""}</section>`;
}
function bindCounterManagement(officeId) {
  const root = $("#counter-list"),
    refresh = () => loadCounterManagement(officeId);
  root.onclick = async (e) => {
    const statusButton = e.target.closest("[data-counter-status]"),
      deactivate = e.target.closest("[data-counter-deactivate]"),
      edit = e.target.closest("[data-counter-edit]"),
      removeService = e.target.closest("[data-service-release]"),
      releaseOfficer = e.target.closest("[data-officer-release]");
    try {
      if (statusButton) {
        await api(
          `/counters/${statusButton.dataset.counterStatus}/${statusButton.dataset.nextStatus}`,
          { method: "POST" },
        );
        notify("Counter updated.");
        return refresh();
      }
      if (
        deactivate &&
        confirm("Deactivate this counter and release its assignments?")
      ) {
        await api(`/counters/${deactivate.dataset.counterDeactivate}`, {
          method: "DELETE",
        });
        notify("Counter deactivated.");
        return refresh();
      }
      if (edit) {
        const code = prompt("Counter code", edit.dataset.code);
        if (code?.trim()) {
          await api(`/counters/${edit.dataset.counterEdit}`, {
            method: "PUT",
            body: { officeId, code },
          });
          notify("Counter updated.");
          return refresh();
        }
      }
      if (removeService) {
        await api(
          `/counters/${removeService.dataset.serviceRelease}/service-assignments/${removeService.dataset.serviceId}`,
          { method: "DELETE" },
        );
        notify("Service released.");
        return refresh();
      }
      if (releaseOfficer) {
        await api(
          `/counters/${releaseOfficer.dataset.officerRelease}/officer-assignment`,
          { method: "DELETE" },
        );
        notify("Officer released.");
        return refresh();
      }
    } catch (error) {
      notify(error.message, true);
    }
  };
  root.onsubmit = async (e) => {
    const serviceForm = e.target.closest("[data-service-assignment]"),
      officerForm = e.target.closest("[data-officer-assignment]");
    if (!serviceForm && !officerForm) return;
    e.preventDefault();
    try {
      if (serviceForm)
        await api("/counters/service-assignments", {
          method: "POST",
          body: {
            counterId: serviceForm.dataset.serviceAssignment,
            serviceId: formData(serviceForm).serviceId,
          },
        });
      if (officerForm)
        await api("/counters/officer-assignments", {
          method: "POST",
          body: {
            counterId: officerForm.dataset.officerAssignment,
            officerId: formData(officerForm).officerId,
          },
        });
      notify("Assignment saved.");
      refresh();
    } catch (error) {
      notify(error.message, true);
    }
  };
}

document.addEventListener(
  "click",
  (event) => {
    const row = event.target.closest("#service-list tr");
    if (!row || event.target.closest("[data-delete]")) return;
    const serviceId = row.querySelector('[data-delete="services"]')?.dataset.id;
    const service = state.services.find((item) => item.publicId === serviceId);
    const form = $("#service-form");
    if (!service || !form) return;
    form.dataset.editServiceId = service.publicId;
    form.elements.departmentId.value = service.departmentId;
    form.elements.name.value = service.name;
    form.elements.dailyCapacity.value = service.dailyCapacity;
    form.elements.startTime.value = String(service.startTime).slice(0, 5);
    form.elements.endTime.value = String(service.endTime).slice(0, 5);
    form.elements.averageServiceMinutes.value = service.averageServiceMinutes;
    form.querySelectorAll('[name="openDays"]').forEach((input) => {
      input.checked = (service.openDays || []).includes(input.value);
    });
    form.querySelector('button[type="submit"], button').textContent =
      "Update service";
    form.scrollIntoView({ behavior: "smooth", block: "center" });
  },
  true,
);

document.addEventListener(
  "submit",
  async (event) => {
    const form = event.target.closest("#service-form");
    if (!form?.dataset.editServiceId) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const data = formData(form);
    data.openDays = new FormData(form).getAll("openDays");
    data.dailyCapacity = Number(data.dailyCapacity);
    data.averageServiceMinutes = Number(data.averageServiceMinutes);
    try {
      await api(`/services/${form.dataset.editServiceId}`, {
        method: "PUT",
        body: data,
      });
      notify("Service updated.");
      await manage();
    } catch (error) {
      notify(error.message, true);
    }
  },
  true,
);

function addServiceEditButtons() {
  document.querySelectorAll("#service-list tr").forEach((row) => {
    if (row.querySelector("[data-service-edit]")) return;
    const deleteButton = row.querySelector('[data-delete="services"]');
    if (!deleteButton) return;
    const editButton = document.createElement("button");
    editButton.type = "button";
    editButton.className = "secondary";
    editButton.dataset.serviceEdit = deleteButton.dataset.id;
    editButton.textContent = "Edit";
    deleteButton.before(editButton);
  });
}

new MutationObserver(addServiceEditButtons).observe(content, {
  childList: true,
  subtree: true,
});

// Accounts are retained for audit and token-history purposes.  Replace the old
// destructive user action with a reversible enable/disable control.
function addUserAccountStateButtons() {
  content.querySelectorAll("[data-user-delete]").forEach((button) => {
    const disabled =
      button.closest("tr")?.children[2]?.textContent.trim() === "DISABLED";
    button.dataset.userAccountId = button.dataset.userDelete;
    button.removeAttribute("data-user-delete");
    button.dataset.userAccountState = disabled ? "enable" : "disable";
    button.classList.toggle("account-enable", disabled);
    button.classList.toggle("danger", !disabled);
    button.textContent = disabled ? "Enable user" : "Disable user";
  });
}

const renderUsersWithAccountStateActions = users;
users = async (...args) => {
  await renderUsersWithAccountStateActions(...args);
  addUserAccountStateButtons();
};

new MutationObserver(addUserAccountStateButtons).observe(content, {
  childList: true,
  subtree: true,
});

document.addEventListener(
  "click",
  async (event) => {
    const button = event.target.closest("[data-user-account-state]");
    if (!button || !content.contains(button)) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const action = button.dataset.userAccountState;
    if (!confirm(`Are you sure you want to ${action} this user account?`))
      return;
    button.disabled = true;
    try {
      await api(`/users/${button.dataset.userAccountId}/${action}`, {
        method: "POST",
      });
      notify(`User account ${action}d successfully.`);
      navigate("users");
    } catch (error) {
      notify(error.message, true);
      button.disabled = false;
    }
  },
  true,
);

document.addEventListener(
  "click",
  (event) => {
    const editButton = event.target.closest("[data-service-edit]");
    if (!editButton) return;
    event.preventDefault();
    const row = editButton.closest("tr");
    row?.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  },
  true,
);
async function adminPost(event, path, done) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  if (event.currentTarget.id === "service-form")
    data.openDays = new FormData(event.currentTarget).getAll("openDays");
  for (const key of ["dailyCapacity", "averageServiceMinutes"])
    if (data[key]) data[key] = Number(data[key]);
  for (const key of ["breakStartTime", "breakEndTime"])
    if (data[key] === "") data[key] = null;
  try {
    await api(path, { method: "POST", body: data });
    notify("Saved.");
    done();
  } catch (e) {
    notify(e.message, true);
  }
}

async function analytics() {
  content.innerHTML = `<section class="card"><h2>Reports and exports</h2><form id="report-form" class="toolbar"><label>From<input name="from" type="date" value="${date()}" required></label><label>To<input name="to" type="date" value="${date()}" required></label><button>Load report</button><button class="secondary" type="button" id="csv">CSV</button><button class="secondary" type="button" id="xlsx">Excel</button></form><div id="report-result"></div></section>`;
  $("#report-form").onsubmit = async (e) => {
    e.preventDefault();
    const q = new URLSearchParams(formData(e.currentTarget));
    try {
      const r = await api(`/analytics/reports?${q}`);
      $("#report-result").innerHTML =
        `<pre class="code">${esc(JSON.stringify(r, null, 2))}</pre>`;
    } catch (x) {
      notify(x.message, true);
    }
  };
  $("#csv").onclick = () => exportReport("report.csv");
  $("#xlsx").onclick = () => exportReport("report.xlsx");
}
function exportReport(file) {
  const q = new URLSearchParams(formData($("#report-form")));
  download(`/analytics/exports/${file}?${q}`, file).catch((e) =>
    notify(e.message, true),
  );
}
async function mutation(path, body, done, method = "POST") {
  try {
    await api(path, { method, body });
    if (typeof done === "function") await done();
  } catch (e) {
    notify(e.message, true);
  }
}
function clearLiveSocket() {
  if (state.liveSocketRetry) clearTimeout(state.liveSocketRetry);
  if (state.liveSocketDebounce) clearTimeout(state.liveSocketDebounce);
  if (state.liveSocket) {
    state.liveSocket.onclose = null;
    state.liveSocket.close();
  }
  state.liveSocket = null;
  state.liveSocketDestination = null;
  state.liveSocketRetry = null;
  state.liveSocketDebounce = null;
  state.liveSocketOnUpdate = null;
}
function connectLiveSocket(destination, subscriptionId, onUpdate) {
  state.liveSocketOnUpdate = onUpdate;
  if (
    state.liveSocketDestination === destination &&
    state.liveSocket?.readyState === WebSocket.OPEN
  )
    return;
  clearLiveSocket();
  state.liveSocketDestination = destination;
  state.liveSocketOnUpdate = onUpdate;
  const connect = () => {
    if (state.tab !== "home" || state.liveSocketDestination !== destination)
      return;
    const protocol = location.protocol === "https:" ? "wss" : "ws";
    const socket = new WebSocket(`${protocol}://${location.host}/ws`);
    state.liveSocket = socket;
    socket.onopen = () =>
      socket.send(
        `CONNECT\naccept-version:1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${state.auth.accessToken}\n\n\0`,
      );
    socket.onmessage = (event) => {
      const frame = String(event.data);
      if (frame.startsWith("CONNECTED")) {
        socket.send(
          `SUBSCRIBE\nid:${subscriptionId}\ndestination:${destination}\n\n\0`,
        );
        return;
      }
      if (!frame.startsWith("MESSAGE")) return;
      clearTimeout(state.liveSocketDebounce);
      state.liveSocketDebounce = setTimeout(
        () => state.liveSocketOnUpdate?.(),
        100,
      );
    };
    socket.onerror = () => socket.close();
    socket.onclose = () => {
      if (state.tab === "home" && state.liveSocketDestination === destination)
        state.liveSocketRetry = setTimeout(connect, 2000);
    };
  };
  connect();
}
function clearOfficerDashboardTimer() {
  if (state.officerRefreshTimer) clearInterval(state.officerRefreshTimer);
  if (state.officerSocketRetry) clearTimeout(state.officerSocketRetry);
  if (state.officerSocket) {
    state.officerSocket.onclose = null;
    state.officerSocket.close();
  }
  state.officerRefreshTimer = null;
  state.officerSocket = null;
  state.officerSocketServiceId = null;
  state.officerSocketRetry = null;
}
function connectOfficerQueueSocket(serviceId, refresh) {
  if (
    state.officerSocketServiceId === serviceId &&
    state.officerSocket?.readyState === WebSocket.OPEN
  )
    return;
  if (state.officerSocket) {
    state.officerSocket.onclose = null;
    state.officerSocket.close();
  }
  state.officerSocketServiceId = serviceId;
  const connect = () => {
    if (state.tab !== "home" || state.officerSocketServiceId !== serviceId)
      return;
    const protocol = location.protocol === "https:" ? "wss" : "ws";
    const socket = new WebSocket(`${protocol}://${location.host}/ws`);
    state.officerSocket = socket;
    socket.onopen = () =>
      socket.send(
        `CONNECT\naccept-version:1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${state.auth.accessToken}\n\n\0`,
      );
    socket.onmessage = (event) => {
      const frame = String(event.data);
      if (!frame.startsWith("CONNECTED")) return;
      if (frame.startsWith("MESSAGE")) {
        const body = frame.slice(frame.indexOf("\n\n") + 2).replace(/\0$/, "");
        try {
          const update = JSON.parse(body);
          if (update.serviceId === serviceId) refresh();
        } catch (_) {}
        return;
      }
      socket.send(
        `SUBSCRIBE\nid:officer-service-${serviceId}\ndestination:/topic/services/${serviceId}\n\n\0`,
      );
    };
    socket.onclose = () => {
      if (state.tab === "home" && state.officerSocketServiceId === serviceId)
        state.officerSocketRetry = setTimeout(connect, 2000);
    };
    socket.onerror = () => socket.close();
  };
  connect();
}
function userDetail(token) {
  return `<p><strong>Name:</strong> ${esc(token.visitorName || "Not recorded")}</p><p><strong>Phone:</strong> ${esc(token.visitorPhone || "Not recorded")}</p><p><strong>Age:</strong> ${token.visitorAge ?? "Not recorded"}</p><p><strong>Gender:</strong> ${esc((token.visitorGender || "Not recorded").replaceAll("_", " "))}</p><p><strong>Arrival:</strong> ${token.appeared ? status("ARRIVED") : "Not arrived"}</p>`;
}
function officerRows(tokens) {
  return tokens.length
    ? `<div class="table-wrap"><table><thead><tr><th>Token</th><th>Time</th><th>Status</th><th>Arrival</th><th></th></tr></thead><tbody>${tokens.map((t) => `<tr><td>#${t.tokenNumber}</td><td>${esc(t.appointmentTime || "-")}</td><td>${status(t.status)}</td><td>${t.appeared ? status("ARRIVED") : "Not arrived"}</td><td><button class="secondary" data-user-detail="${t.publicId}">User details</button><button data-token="${t.publicId}" data-token-label="Token #${t.tokenNumber} · ${esc(t.serviceName)}">Manage</button></td></tr>`).join("")}</tbody></table></div>`
    : '<p class="empty">No tokens match these filters.</p>';
}
async function officerHome() {
  const assigned = await api("/officer/counters");
  const counter = assigned[0];
  if (!counter) {
    content.innerHTML =
      '<section class="card"><h2>No assigned counter</h2><p class="empty">Contact an administrator for an active counter assignment.</p></section>';
    return;
  }
  const states = [
    "WAITING",
    "CALLED",
    "SKIPPED",
    "COMPLETED",
    "CANCELLED",
    "NO_SHOW",
  ];
  content.innerHTML = `<section class="card"><p class="eyebrow">Officer dashboard</p><h2>Live queue for your counter</h2><div class="grid officer-overview"><div class="span-4">Assigned counter<strong>${esc(counter.counterCode)}</strong></div><div class="span-4">Assigned service<strong id="assigned-service">${esc(counter.services[0]?.serviceName || "None")}</strong></div><div class="span-4">Status<strong id="counter-status">Loading…</strong></div></div></section><section class="card"><form id="officer-filters" class="toolbar"><label>Date<input type="date" name="date" value="${date()}"></label><label>Service<select name="serviceId">${counter.services.map((s) => `<option value="${s.serviceId}">${esc(s.serviceName)}</option>`).join("")}</select></label><fieldset class="status-filter"><legend>Status</legend>${states.map((s) => `<label><input type="checkbox" name="statuses" value="${s}">${s.replace("_", " ")}</label>`).join("")}</fieldset><label>Arrival<select name="arrived"><option value="">All</option><option value="true">Arrived</option><option value="false">Not arrived</option></select></label></form></section><div id="officer-dashboard" class="loading">Loading live queue…</div><section id="officer-user-panel" class="card hidden"></section>`;
  const form = $("#officer-filters");
  const load = async () => {
    const data = new FormData(form),
      p = new URLSearchParams({
        date: data.get("date"),
        serviceId: data.get("serviceId"),
      });
    data.getAll("statuses").forEach((s) => p.append("statuses", s));
    if (data.get("arrived")) p.set("arrived", data.get("arrived"));
    try {
      const dashboard = await api(
        `/officer/counters/${counter.counterId}/dashboard?${p}`,
      );
      const waiting = dashboard.tokens.filter((t) =>
        ["WAITING", "SKIPPED"].includes(t.status),
      );
      $("#assigned-service").textContent = dashboard.serviceName;
      $("#counter-status").innerHTML = status(dashboard.counterStatus);
      $("#officer-dashboard").innerHTML =
        `<div class="grid"><section class="card span-5"><p class="eyebrow">Current token</p>${dashboard.currentToken ? `<h2>Now serving #${dashboard.currentToken.tokenNumber}</h2><p>${status(dashboard.currentToken.status)} · ${esc(dashboard.currentToken.visitorName)}</p><button class="secondary" data-user-detail="${dashboard.currentToken.publicId}">View user details</button>` : '<h2>No token is being served</h2><p class="muted">Call the next token from Token operations.</p>'}</section><section class="card span-7"><p class="eyebrow">Today's statistics</p><div class="stats-row"><div>Completed<strong>${dashboard.completedCount}</strong></div><div>Cancelled<strong>${dashboard.cancelledCount}</strong></div><div>Average wait<strong>${dashboard.averageWaitMinutes} min</strong></div></div><p class="muted">${dashboard.arrivedCount} arrived · ${dashboard.waitingCount} waiting</p></section><section class="card span-12"><p class="eyebrow">Waiting queue</p><h2>${esc(dashboard.queueDate)} live queue</h2>${officerRows(waiting)}</section><section class="card span-12"><p class="eyebrow">Filtered tokens</p><h2>All matching tokens</h2>${officerRows(dashboard.tokens)}</section></div>`;
      $("#officer-dashboard").onclick = (e) => {
        const detail = e.target.closest("[data-user-detail]"),
          manage = e.target.closest("[data-token]");
        if (detail) {
          const t =
            dashboard.tokens.find(
              (x) => x.publicId === detail.dataset.userDetail,
            ) ||
            (dashboard.currentToken?.publicId === detail.dataset.userDetail
              ? dashboard.currentToken
              : null);
          if (t) {
            $("#officer-user-panel").classList.remove("hidden");
            $("#officer-user-panel").innerHTML =
              `<div class="actions"><h2>User details — token #${t.tokenNumber}</h2><button class="secondary" id="close-user-detail">Close</button></div>${userDetail(t)}`;
            $("#close-user-detail").onclick = () =>
              $("#officer-user-panel").classList.add("hidden");
          }
        }
        if (manage) {
          state.selectedToken = manage.dataset.token;
          state.selectedTokenLabel = manage.dataset.tokenLabel;
          navigate("operations");
        }
      };
    } catch (e) {
      $("#officer-dashboard").innerHTML =
        `<section class="card"><p class="empty">${esc(e.message)}</p></section>`;
    }
  };
  form.onchange = load;
  await load();
  state.officerRefreshTimer = setInterval(
    () => state.tab === "home" && load(),
    15000,
  );
}
async function operations() {
  if (state.auth.role !== "OFFICER") return adminHome();
  const assigned = await api("/officer/counters");
  const fixedCounter = assigned[0];
  content.innerHTML = `<section class="card"><h2>Serve next token</h2>${assigned.length ? `<form id="next-form" class="form-grid"><p class="full"><strong>Assigned counter:</strong> ${esc(fixedCounter.counterCode)} · ${esc(fixedCounter.officeName)}</p><input type="hidden" name="counterId" value="${fixedCounter.counterId}"><label>Service<select id="operation-service" name="serviceId"></select></label><button class="full">Call next</button></form>` : '<p class="muted">No active counter is assigned to you.</p>'}<div id="active-operation"></div></section><section class="card"><h2>Operate a token</h2>${assigned.length ? `<form id="operation-form" class="form-grid">${state.selectedToken ? `<p class="full"><strong>Selected:</strong> ${esc(state.selectedTokenLabel)}</p><input type="hidden" name="tokenId" value="${esc(state.selectedToken)}">` : '<p class="full muted">Select a live token or a token from User history first.</p>'}<input type="hidden" name="counterId" value="${fixedCounter.counterId}"><label>Action<select name="action"><option value="arrive">Mark patient arrived</option><option value="complete">Complete</option><option value="skip">Skip</option><option value="recall">Recall</option><option value="no-show">Mark no-show</option></select></label><button ${state.selectedToken ? "" : "disabled"}>Apply action</button></form>` : ""}</section>`;
  if (!assigned.length) return;
  $("#operation-service").innerHTML = fixedCounter.services
    .map(
      (service) =>
        `<option value="${service.serviceId}">${esc(service.serviceName)}</option>`,
    )
    .join("");
  $("#next-form").onsubmit = async (e) => {
    e.preventDefault();
    try {
      const token = await api("/tokens/next", {
        method: "POST",
        body: formData(e.currentTarget),
      });
      $("#active-operation").innerHTML =
        `<p>Now serving token <strong>#${token.tokenNumber}</strong> (${status(token.status)})</p>`;
      notify("Next token called.");
    } catch (error) {
      notify(error.message, true);
    }
  };
  $("#operation-form").onsubmit = async (e) => {
    e.preventDefault();
    const { tokenId, counterId, action } = formData(e.currentTarget);
    await mutation(`/tokens/${tokenId}/${action}`, { counterId }, () =>
      notify(
        action === "arrive"
          ? "Patient marked as arrived."
          : `Token ${action}d.`,
      ),
    );
  };
}
async function loadCounterManagement(officeId) {
  const target = $("#counter-list");
  if (!officeId) return;
  target.innerHTML = $("#loading").innerHTML;
  try {
    const [counters, departments, people, history] = await Promise.all([
      api(`/counters/management?officeId=${officeId}`),
      api(`/departments?officeId=${officeId}`),
      api("/users"),
      api(`/counters/officer-assignment-history?officeId=${officeId}`),
    ]);
    const groups = await Promise.all(
      departments.map((d) => api(`/services?departmentId=${d.publicId}`)),
    );
    const services = groups.flat(),
      officers = people.filter((p) => p.role === "OFFICER" && p.enabled),
      departmentNames = new Map(departments.map((d) => [d.publicId, d.name]));
    const countersMarkup = counters.length
      ? counters
          .map((c) =>
            counterManagementCard(
              c,
              officeId,
              officers,
              services,
              departmentNames,
            ),
          )
          .join("")
      : '<p class="empty">No counters for this office yet.</p>';
    target.innerHTML = `${countersMarkup}${officerAssignmentHistory(history)}`;
    bindCounterManagement(officeId);
  } catch (error) {
    target.innerHTML =
      '<p class="empty">Could not load counter management.</p>';
    notify(error.message, true);
  }
}
function officerAssignmentHistory(assignments) {
  return `<section class="card"><h3>Officer assignment history</h3>${assignments.length ? `<div class="table-wrap"><table><thead><tr><th>Officer</th><th>Counter</th><th>Assigned</th><th>Released</th><th>Status</th></tr></thead><tbody>${assignments.map((assignment) => `<tr><td>${esc(assignment.officerEmail)}</td><td>${esc(assignment.counterCode)}</td><td>${esc(new Date(assignment.assignedAt).toLocaleString())}</td><td>${assignment.releasedAt ? esc(new Date(assignment.releasedAt).toLocaleString()) : "-"}</td><td>${assignment.releasedAt ? status("RELEASED") : status("ACTIVE")}</td></tr>`).join("")}</tbody></table></div>` : '<p class="muted">No officer assignments recorded for this office.</p>'}</section>`;
}
function serviceDetails(token) {
  const service =
    token.serviceName ||
    state.serviceNames[token.serviceId] ||
    "Archived service";
  const department = token.departmentName || "Archived department";
  const office = token.officeName || "Archived office";
  return `${esc(service)}<br><small>${esc(department)} · ${esc(office)}${token.officeAddress ? ` — ${esc(token.officeAddress)}` : ""}</small>`;
}
function tokenTable(tokens = [], selectable = false) {
  return tokens.length
    ? `<div class="table-wrap"><table><thead><tr><th>Token</th><th>Date</th><th>Time</th><th>State</th><th>Service details</th>${selectable ? "<th></th>" : ""}</tr></thead><tbody>${tokens
        .map((token) => {
          const manageable = ["WAITING", "CALLED", "SKIPPED"].includes(
            token.status,
          );
          const serviceLabel =
            token.serviceName ||
            state.serviceNames[token.serviceId] ||
            "Archived service";
          return `<tr><td>#${token.tokenNumber}</td><td>${esc(token.queueDate)}</td><td>${esc(token.appointmentTime || "-")}</td><td>${status(token.status)}</td><td>${serviceDetails(token)}</td>${selectable ? `<td>${manageable ? `<button data-token="${token.publicId}" data-token-status="${token.status}" data-token-service-id="${token.serviceId}" data-token-label="Token #${token.tokenNumber} · ${esc(serviceLabel)}">Manage</button>` : '<span class="muted">Closed</span>'}</td>` : ""}</tr>`;
        })
        .join("")}</tbody></table></div>`
    : '<p class="empty">Nothing to show yet.</p>';
}
// Handle this at capture time so a second pointer or keyboard submit cannot reach
// the operation handler while the first "Call next" request is in progress.
document.addEventListener(
  "submit",
  async (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement) || form.id !== "next-form") return;
    event.preventDefault();
    event.stopImmediatePropagation();
    if (form.dataset.callingNext === "true") return;

    form.dataset.callingNext = "true";
    const button = form.querySelector("button");
    if (button) button.disabled = true;
    try {
      const token = await api("/tokens/next", {
        method: "POST",
        body: formData(form),
      });
      $("#active-operation").innerHTML =
        `<p>Now serving token <strong>#${token.tokenNumber}</strong> (${status(token.status)})</p>`;
      notify("Next token called.");
    } catch (error) {
      notify(error.message, true);
    } finally {
      delete form.dataset.callingNext;
      if (button) button.disabled = false;
    }
  },
  true,
);

setup();

function appointmentMessage(token, minutes) {
  if (token.status === "CALLED") return "Now Serving You";
  if (token.status === "NO_SHOW") return "Token Missed";
  if (token.status === "COMPLETED") return "Service Completed";
  if (minutes <= 5) return "Almost Your Turn";
  if (minutes <= 30) return "Please proceed to the service area";
  return "Waiting in Queue";
}

function formatWaitDuration(minutes) {
  const total = Math.max(0, Math.round(minutes));
  if (total < 60) return `${total} ${total === 1 ? "minute" : "minutes"}`;
  const hours = Math.floor(total / 60),
    remainder = total % 60;
  return `${hours} ${hours === 1 ? "hr" : "hrs"}${remainder ? ` ${remainder} min` : ""}`;
}
function appointmentEstimate(token, minutes) {
  if (!token.appointmentTime) return formatWaitDuration(minutes);
  const appointment = new Date(`${token.queueDate}T${token.appointmentTime}`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const appointmentDay = new Date(appointment);
  appointmentDay.setHours(0, 0, 0, 0);
  const daysAway = Math.round((appointmentDay - today) / 86400000);
  const time = String(token.appointmentTime).slice(0, 5);
  if (daysAway === 1) return `Tomorrow at ${time}`;
  if (daysAway > 1)
    return `${appointment.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" })} at ${time}`;
  return formatWaitDuration(minutes);
}

function stopAppointmentTimers() {
  if (state.waitRefreshTimer) clearTimeout(state.waitRefreshTimer);
  if (state.clockTimer) clearInterval(state.clockTimer);
  if (state.citizenSyncTimer) clearInterval(state.citizenSyncTimer);
  state.waitRefreshTimer = null;
  state.clockTimer = null;
  state.citizenSyncTimer = null;
}

function updateClock() {
  const clock = $("#current-time");
  if (clock)
    clock.textContent = new Intl.DateTimeFormat(undefined, {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    }).format(new Date());
}

// Keep the citizen's appointment authoritative even if a browser misses a WebSocket frame.
const renderCitizenHome = citizenHome;
citizenHome = async function () {
  await renderCitizenHome();
  const citizenId = authUserId();
  if (citizenId)
    connectLiveSocket(
      `/topic/citizens/${citizenId}`,
      "citizen-queue",
      () => state.tab === "home" && citizenHome(),
    );
  if (state.citizenSyncTimer) clearInterval(state.citizenSyncTimer);
  state.citizenSyncTimer = setInterval(async () => {
    if (state.tab !== "home" || state.auth?.role !== "CITIZEN") return;
    const latest = await api("/tokens/active").catch(() => null);
    const previous = state.activeToken;
    if (
      (latest?.publicId || null) !== (previous?.publicId || null) ||
      latest?.appeared !== previous?.appeared ||
      latest?.counterCode !== previous?.counterCode ||
      latest?.status !== previous?.status
    )
      await citizenHome();
  }, 3000);
};

async function tokenInfo(id) {
  const current = await api("/tokens/active").catch(() => state.activeToken);
  if (!current || current.publicId !== id || state.tab !== "home") return;
  state.activeToken = current;
  const wait = await api(`/tokens/${id}/wait-time`);
  const now = Date.now();
  const elapsedMinutes = state.waitUpdatedAt
    ? Math.floor((now - state.waitUpdatedAt) / 60000)
    : 0;
  const serverEstimate =
    current.status === "WAITING" || current.status === "SKIPPED"
      ? wait.estimatedMinutes
      : 0;
  const locallyReduced =
    state.waitDisplayMinutes == null
      ? serverEstimate
      : Math.max(0, state.waitDisplayMinutes - elapsedMinutes);
  const remaining = Math.min(serverEstimate, locallyReduced);
  state.waitDisplayMinutes = remaining;
  state.waitUpdatedAt = now;
  const refreshMinutes = remaining > 15 ? 5 : 2;
  const statusNode = $("#appointment-status");
  const messageNode = $("#appointment-message");
  if (statusNode) statusNode.innerHTML = status(current.status);
  if (messageNode)
    messageNode.textContent = appointmentMessage(current, remaining);
  const info = $("#token-info");
  if (info)
    info.innerHTML = `<p><strong>${wait.peopleAhead}</strong> people ahead &middot; approx. <strong>${esc(appointmentEstimate(current, remaining))}</strong></p><p class="muted">Estimate refreshes every ${refreshMinutes} minutes.</p>`;
  state.waitRefreshTimer = window.setTimeout(
    () => tokenInfo(id).catch((error) => notify(error.message, true)),
    refreshMinutes * 60000,
  );
}
async function citizenHome() {
  stopAppointmentTimers();
  state.waitDisplayMinutes = null;
  state.waitUpdatedAt = null;
  const active = await api("/tokens/active").catch(() => null);
  state.activeToken = active;
  const canCancel = active?.status === "WAITING";
  content.innerHTML = `<div class="grid"><section class="card span-7"><p class="eyebrow">My appointment</p><h2>${active ? esc(active.serviceName || "Appointment details") : "No active appointment"}</h2>${active ? `<p id="appointment-status">${status(active.status)}</p><h3 id="appointment-message">Waiting in Queue</h3><div class="stack"><p><strong>Token number:</strong> #${active.tokenNumber}</p><p><strong>Location:</strong> ${esc(active.officeName)}${active.officeAddress ? ` &mdash; ${esc(active.officeAddress)}` : ""}</p><p><strong>Department:</strong> ${esc(active.departmentName)}</p><p><strong>Service:</strong> ${esc(active.serviceName)}</p><p><strong>Counter:</strong> ${active.counterCode ? esc(active.counterCode) : '<span class="muted">Will be assigned when the officer calls you</span>'}</p><p><strong>Date and time:</strong> ${esc(active.queueDate)}${active.appointmentTime ? ` at ${esc(active.appointmentTime)}` : ""}</p><p><strong>Booked for:</strong> ${esc(active.visitorName || "Not recorded")}${active.visitorPhone ? ` &middot; ${esc(active.visitorPhone)}` : ""}${active.visitorAge != null ? ` &middot; Age ${esc(active.visitorAge)}` : ""}${active.visitorGender ? ` &middot; ${esc(active.visitorGender.replaceAll("_", " ").toLowerCase())}` : ""}</p><p><strong>Arrival:</strong> ${active.appeared ? '<span class="status">ARRIVED</span>' : '<span class="muted">Awaiting officer confirmation</span>'}</p></div><div class="actions"><button id="details">Refresh wait time</button>${canCancel ? '<button class="secondary" id="cancel">Cancel appointment</button>' : ""}</div><div id="token-info" class="stack"><p class="muted">Calculating your approximate wait time&hellip;</p></div>` : '<p class="muted">Choose a service to book your next visit.</p><button id="go-book">Book an appointment</button>'}</section></div>`;
  $("#go-book")?.addEventListener("click", () => navigate("book"));
  if (active) {
    $("#details").onclick = () => {
      state.waitDisplayMinutes = null;
      tokenInfo(active.publicId).catch((error) => notify(error.message, true));
    };
    $("#cancel")?.addEventListener("click", () =>
      mutation(`/tokens/${active.publicId}/cancel`, {}, citizenHome),
    );
    await tokenInfo(active.publicId).catch((error) => {
      $("#token-info").innerHTML =
        '<p class="muted">Unable to calculate the wait time right now. Use Refresh wait time to try again.</p>';
      notify(error.message, true);
    });
  }
}
