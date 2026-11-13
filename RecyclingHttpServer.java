package ug.dankk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

final class RecyclingHttpServer {
    private static final int PORT = 8765;
    private static final int MAX_REQUEST_BYTES = 8_192;
    private static final List<String> MATERIALS =
            List.of("Plastic", "Paper / Cardboard", "Glass", "Metal", "E-waste", "Other");
    private static final String PAGE = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="theme-color" content="#123b32">
              <title>DAN KK Recycling Centre | Intake Dashboard</title>
              <style>
                :root { color-scheme:light; --green:#17624c; --green-dark:#123b32;
                  --green-deep:#0c2d28; --teal:#168c83; --mint:#e7f3ed;
                  --gold:#d8a447; --ink:#20362f; --muted:#718079;
                  --line:#e1e9e3; --surface:#fff; --bg:#f5f7f3; --shadow:0 12px 32px #183c2d0c; }
                * { box-sizing:border-box; }
                body { margin:0; min-height:100vh;
                  background:radial-gradient(ellipse at 92% 5%,#e5efe7 0,transparent 28%),var(--bg);
                  color:var(--ink);
                  font:15px/1.5 "Segoe UI",Arial,sans-serif; }
                main { max-width:1240px; margin:auto; padding:34px 28px 48px; }
                header { margin-bottom:27px; padding:4px 0 18px;
                  border-bottom:1px solid #dce6df; }
                h1 { margin:0; color:var(--green-dark); font-size:clamp(25px,4vw,34px);
                  letter-spacing:-.7px; }
                header p { margin:6px 0 0; color:var(--muted); }
                nav { display:flex; flex-wrap:wrap; gap:8px; margin-top:20px; }
                nav a { display:inline-flex; align-items:center; min-height:38px; padding:0 15px;
                  color:#53675d; background:#ffffffc9; border:1px solid transparent;
                  border-radius:8px; font-size:14px; font-weight:650; text-decoration:none;
                  transition:background .18s,color .18s,transform .18s,border-color .18s; }
                nav a:hover { color:var(--green-dark); background:#fff; border-color:var(--line);
                  transform:translateY(-1px); }
                nav a[aria-current="page"] { color:#fff; background:var(--green-dark);
                  box-shadow:0 4px 12px #123b3226; }
                .hero { position:relative; overflow:hidden; margin:10px 0 22px; padding:40px;
                  border:1px solid #ffffff16; border-radius:18px; color:#fff;
                  background:linear-gradient(116deg,var(--green-deep) 0%,#145b48 62%,#168277 100%);
                  box-shadow:0 18px 40px #123b3226; }
                .hero:after { position:absolute; content:""; width:290px; height:290px; right:-90px;
                  top:-120px; border:1px solid #ffffff30; border-radius:50%;
                  box-shadow:0 0 0 30px #ffffff0d,0 0 0 60px #ffffff0a,
                    0 0 0 90px #ffffff08; }
                .hero .eyebrow { color:#d9c18e; }
                .hero h2 { max-width:690px; margin:12px 0; font-size:clamp(30px,5vw,48px);
                  line-height:1.12; letter-spacing:-1px; }
                .hero p { max-width:640px; margin:0; color:#e1f1e8; font-size:16px; }
                .hero-actions { display:flex; flex-wrap:wrap; gap:10px; margin-top:24px; }
                .hero-actions a,.feature-link { display:inline-flex; align-items:center;
                  justify-content:center; min-height:43px; padding:0 16px; border-radius:7px;
                  color:var(--green-dark); background:#fff; font-weight:700; text-decoration:none;
                  box-shadow:0 3px 10px #072b221e; transition:transform .18s,box-shadow .18s; }
                .hero-actions a:hover,.feature-link:hover { transform:translateY(-2px);
                  box-shadow:0 7px 16px #072b222b; }
                .hero-actions a.secondary { color:#fff; background:#ffffff1c;
                  border:1px solid #ffffff70; }
                .section-heading { margin:28px 0 13px; }
                .section-heading h2 { margin:0; font-size:21px; }
                .section-heading p { margin:4px 0 0; color:var(--muted); }
                .feature-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:14px; }
                .feature { padding:20px; background:#fff; border:1px solid var(--line);
                  border-radius:12px; box-shadow:var(--shadow);
                  transition:transform .18s,box-shadow .18s,border-color .18s; }
                .feature:hover { transform:translateY(-3px); border-color:#b8d4c5;
                  box-shadow:0 15px 32px #183c2d14; }
                .feature-icon { display:grid; place-items:center; width:40px; height:40px;
                  color:var(--green); background:linear-gradient(145deg,#e7f3ed,#d8eeea);
                  border-radius:10px;
                  font-size:19px; font-weight:750; }
                .feature h3 { margin:13px 0 6px; font-size:17px; }
                .feature p { margin:0 0 15px; color:var(--muted); font-size:14px; }
                .text-link { color:var(--green); font-weight:700; text-decoration:none; }
                .page-intro { margin:6px 0 20px; }
                .page-intro h2 { margin:0 0 5px; font-size:28px; }
                .page-intro p { max-width:760px; margin:0; color:var(--muted); }
                .about-layout { display:grid; grid-template-columns:1.4fr 1fr; gap:16px; }
                .content-card { padding:24px; background:#fff; border:1px solid var(--line);
                  border-radius:12px; box-shadow:var(--shadow); }
                .content-card h3 { margin:0 0 8px; }
                .content-card p { color:var(--muted); }
                .value-list { display:grid; gap:10px; margin:14px 0 0; padding:0; list-style:none; }
                .value-list li { padding:11px 13px; background:#f2f7f2; border-left:3px solid var(--teal);
                  border-radius:0 8px 8px 0; }
                .service-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:14px; }
                .cards { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; }
                .card,.panel { background:var(--surface); border:1px solid var(--line);
                  border-radius:13px; box-shadow:var(--shadow); }
                .card { position:relative; overflow:hidden; min-height:130px; padding:20px 22px; }
                .card:before { position:absolute; content:""; inset:0 auto 0 0; width:4px;
                  background:linear-gradient(180deg,var(--teal),var(--green)); }
                .card:nth-child(2):before { background:linear-gradient(180deg,#d8a447,#edc978); }
                .card:nth-child(3):before { background:linear-gradient(180deg,#588da0,#78b6b3); }
                .eyebrow { color:var(--muted); font-size:12px; font-weight:700; letter-spacing:.07em; }
                .stat { color:var(--green); font-size:30px; font-weight:750; margin:7px 0 0; }
                .detail { color:var(--muted); font-size:13px; margin:0; }
                .workspace { display:grid; grid-template-columns:320px minmax(0,1fr); gap:18px; margin-top:20px; }
                .panel { padding:22px; min-width:0; }
                h2 { font-size:19px; margin:0 0 17px; }
                label { display:block; font-size:13px; font-weight:650; margin:13px 0 5px; }
                input,select { width:100%; height:42px; border:1px solid #cbd8cf; border-radius:8px;
                  padding:0 11px; background:#fff; color:var(--ink); font:inherit; }
                input:focus,select:focus { outline:3px solid #b8ddd0; border-color:var(--teal); }
                button { margin-top:19px; width:100%; min-height:44px; border:0; border-radius:8px;
                  color:#fff; background:linear-gradient(110deg,var(--green),var(--teal));
                  font:inherit; font-weight:700; cursor:pointer;
                  box-shadow:0 4px 11px #17624c24; transition:filter .18s,transform .18s; }
                button:hover { filter:brightness(.94); transform:translateY(-1px); }
                button:disabled { opacity:.65; cursor:wait; transform:none; }
                .status { min-height:25px; margin:12px 0 0; color:var(--muted); font-size:13px; }
                .status.error { color:#a12626; } .status.success { color:var(--green); }
                .table-wrap { overflow:auto; }
                table { width:100%; border-collapse:collapse; white-space:nowrap; font-size:13px; }
                th,td { padding:12px 10px; text-align:left; border-bottom:1px solid var(--line); }
                th { background:#edf4ef; color:#526c60; font-size:11px; letter-spacing:.05em;
                  text-transform:uppercase; }
                tbody tr:nth-child(even) { background:#fbfcfa; }
                tbody tr:hover { background:#edf6f1; }
                .empty { color:var(--muted); padding:24px 8px; text-align:center; }
                [hidden] { display:none !important; }
                .supplier-tools { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
                .supplier-tools input { max-width:340px; }
                .supplier-form { display:flex; align-items:end; gap:12px; margin:0 0 20px; }
                .supplier-form label { flex:1; margin:0; }
                .supplier-form button { width:auto; margin:0; padding:0 16px; }
                .action-buttons { display:flex; gap:7px; }
                .action-buttons button { width:auto; min-height:32px; margin:0; padding:0 10px;
                  font-size:12px; }
                .action-buttons .secondary { color:var(--green); background:#eaf4ee; }
                .action-buttons .danger { color:#a12626; background:#fff0f0; }
                .inactive { color:var(--muted); font-size:13px; }
                @media (max-width:850px) { main { padding:24px 16px 36px; }
                  .workspace { grid-template-columns:1fr; } .cards { gap:10px; }
                  .card { padding:16px; min-height:116px; } .stat { font-size:24px; }
                  .feature-grid,.service-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
                  .about-layout { grid-template-columns:1fr; } }
                @media (max-width:550px) { .cards { grid-template-columns:1fr; }
                  .card { min-height:0; } .stat { font-size:27px; } .panel { padding:17px; }
                  .hero { padding:26px 20px; } .feature-grid,.service-grid { grid-template-columns:1fr; }
                  nav { gap:6px; } nav a { padding:0 10px; font-size:13px; } }
              </style>
            </head>
            <body>
              <main>
                <header>
                  <h1>DAN KK Recycling Centre</h1>
                  <p>Local recycling services, intake records, and community partnerships.</p>
                  <nav aria-label="Main navigation">
                    <a href="#home" data-page-link="home">Home</a>
                    <a href="#about" data-page-link="about">About us</a>
                    <a href="#services" data-page-link="services">Services</a>
                    <a href="#dashboard" data-page-link="dashboard">Dashboard</a>
                    <a href="#suppliers" data-page-link="suppliers">Suppliers</a>
                  </nav>
                </header>
                <section id="page-home" class="page">
                  <section class="hero">
                    <div class="eyebrow">DAN KK RECYCLING CENTRE</div>
                    <h2>Turn today's recyclable materials into a cleaner tomorrow.</h2>
                    <p>A practical local hub for responsible material recovery, transparent intake
                      records, and a stronger circular economy in our community.</p>
                    <div class="hero-actions">
                      <a href="#dashboard" data-page-link="dashboard">Record a material intake</a>
                      <a class="secondary" href="#services" data-page-link="services">Explore our services</a>
                    </section>
                  </section>
                  <section class="cards" aria-label="Live centre totals">
                    <article class="card"><div class="eyebrow">MATERIAL RECOVERED</div>
                      <p class="stat" id="home-weight">—</p><p class="detail">kilograms recorded</p></article>
                    <article class="card"><div class="eyebrow">COMMUNITY INTAKES</div>
                      <p class="stat" id="home-count">—</p><p class="detail">all time</p></article>
                    <article class="card"><div class="eyebrow">ACTIVE PARTNERS</div>
                      <p class="stat" id="home-suppliers">—</p><p class="detail">suppliers and collectors</p></article>
                  </section>
                  <div class="section-heading">
                    <h2>Explore the centre</h2>
                    <p>Everything you need to work with the recycling centre in one place.</p>
                  </div>
                  <section class="feature-grid">
                    <article class="feature"><div class="feature-icon">01</div>
                      <h3>Responsible recycling</h3>
                      <p>Learn how the centre receives and handles recyclable materials.</p>
                      <a class="text-link" href="#services" data-page-link="services">View services →</a>
                    </article>
                    <article class="feature"><div class="feature-icon">02</div>
                      <h3>Simple material intake</h3>
                      <p>Record a delivery, its weight, and agreed rate with a clear saved record.</p>
                      <a class="text-link" href="#dashboard" data-page-link="dashboard">Open intake dashboard →</a>
                    </article>
                    <article class="feature"><div class="feature-icon">03</div>
                      <h3>Community partnerships</h3>
                      <p>Keep supplier and collector details organized and see their contribution.</p>
                      <a class="text-link" href="#suppliers" data-page-link="suppliers">Manage partners →</a>
                    </article>
                  </section>
                </section>
                <section id="page-about" class="page" hidden>
                  <div class="page-intro">
                    <h2>About DAN KK Recycling Centre</h2>
                    <p>Building cleaner communities through practical recycling, trusted partnerships,
                      and responsible material recovery.</p>
                  </div>
                  <section class="about-layout">
                    <article class="content-card">
                      <h3>Our purpose</h3>
                      <p>DAN KK Recycling Centre provides a local place for recyclable materials to be
                        received, weighed, recorded, and directed toward recovery instead of waste.
                        We make each intake traceable so the people bringing materials and the centre
                        can work from a clear record.</p>
                      <h3>Our approach</h3>
                      <p>We value accurate weighing, transparent rates, respectful service, and
                        collaboration with suppliers and collectors. The system keeps intake history
                        on this computer so centre records remain available offline.</p>
                      <a class="text-link" href="#dashboard" data-page-link="dashboard">
                        Start with a material intake →
                      </a>
                    </article>
                    <aside class="content-card">
                      <h3>What guides us</h3>
                      <ul class="value-list">
                        <li><strong>Responsibility</strong><br>Recover materials thoughtfully.</li>
                        <li><strong>Transparency</strong><br>Record weight, rate, and value clearly.</li>
                        <li><strong>Community</strong><br>Work with local suppliers and collectors.</li>
                        <li><strong>Continuity</strong><br>Keep centre records locally and securely.</li>
                      </ul>
                    </aside>
                  </section>
                </section>
                <section id="page-services" class="page" hidden>
                  <div class="page-intro">
                    <h2>Our services</h2>
                    <p>Bring recyclable materials to the centre, record every intake, and follow
                      contributions over time. Select a service to continue to the relevant page.</p>
                  </div>
                  <section class="service-grid">
                    <article class="feature"><div class="feature-icon">♻</div>
                      <h3>Material drop-off and intake</h3>
                      <p>Register the supplier or collector, choose a material, and enter the verified
                        weight and agreed rate per kilogram. A saved intake updates centre totals.</p>
                      <a class="feature-link" href="#dashboard" data-page-link="dashboard">
                        Record an intake
                      </a>
                    </article>
                    <article class="feature"><div class="feature-icon">KG</div>
                      <h3>Weighing and value records</h3>
                      <p>Keep a consistent digital record of weights, rates, estimated values, and
                        intake dates for review at the centre.</p>
                      <a class="feature-link" href="#dashboard" data-page-link="dashboard">
                        View intake records
                      </a>
                    </article>
                    <article class="feature"><div class="feature-icon">+</div>
                      <h3>Supplier and collector partnerships</h3>
                      <p>Maintain partner names, see their recorded contribution, and keep supplier
                        history connected to every intake.</p>
                      <a class="feature-link" href="#suppliers" data-page-link="suppliers">
                        Manage suppliers
                      </a>
                    </article>
                  </section>
                  <div class="section-heading">
                    <h2>Materials accepted</h2>
                    <p>Choose the appropriate category when recording an intake.</p>
                  </div>
                  <section class="feature-grid">
                    <article class="feature"><h3>Plastic</h3><p>Plastic recyclable materials.</p></article>
                    <article class="feature"><h3>Paper / Cardboard</h3><p>Paper and cardboard materials.</p></article>
                    <article class="feature"><h3>Glass</h3><p>Glass materials collected for recovery.</p></article>
                    <article class="feature"><h3>Metal</h3><p>Metal recyclable materials.</p></article>
                    <article class="feature"><h3>E-waste</h3><p>Electronic waste materials for responsible handling.</p></article>
                    <article class="feature"><h3>Other</h3><p>Other materials accepted by the centre.</p></article>
                  </section>
                </section>
                <section id="page-dashboard" class="page">
                <section class="cards" aria-label="Centre totals">
                  <article class="card"><div class="eyebrow">TOTAL MATERIAL RECEIVED</div>
                    <p class="stat" id="total-weight">—</p><p class="detail">kilograms</p></article>
                  <article class="card"><div class="eyebrow">INTAKE TRANSACTIONS</div>
                    <p class="stat" id="transaction-count">—</p><p class="detail">all time</p></article>
                  <article class="card"><div class="eyebrow">ESTIMATED MATERIAL VALUE</div>
                    <p class="stat" id="total-value">—</p><p class="detail">all time</p></article>
                </section>
                <section class="workspace">
                  <form class="panel" id="intake-form">
                    <h2>Register material intake</h2>
                    <label for="supplier">Supplier / collector</label>
                    <input id="supplier" name="supplier" maxlength="160" autocomplete="organization"
                      list="supplier-options" required>
                    <datalist id="supplier-options"></datalist>
                    <label for="material">Material type</label>
                    <select id="material" name="material">
                      <option>Plastic</option><option>Paper / Cardboard</option><option>Glass</option>
                      <option>Metal</option><option>E-waste</option><option>Other</option>
                    </select>
                    <label for="weight">Weight (kg)</label>
                    <input id="weight" name="weightKg" type="number" min="0.001" step="0.001" required>
                    <label for="rate">Rate per kg</label>
                    <input id="rate" name="ratePerKg" type="number" min="0.01" step="0.01" required>
                    <button id="save-button" type="submit">Save intake record</button>
                    <p id="status" class="status" role="status" aria-live="polite">
                      Enter an intake record to get started.</p>
                  </form>
                  <section class="panel" aria-labelledby="history-heading">
                    <h2 id="history-heading">Recent intake</h2>
                    <div class="table-wrap">
                      <table>
                        <thead><tr><th>Date &amp; time</th><th>Supplier</th><th>Material</th>
                          <th>Weight (kg)</th><th>Rate / kg</th><th>Value</th></tr></thead>
                        <tbody id="transactions"></tbody>
                      </table>
                      <p id="empty-state" class="empty" hidden>No intake records yet.</p>
                    </div>
                  </section>
                </section>
                </div>
                <section id="page-suppliers" class="page" hidden>
                  <section class="panel">
                    <h2>Suppliers and collectors</h2>
                    <p class="detail">Manage supplier names used for intake. Renaming updates their
                      historical records. Suppliers with intake history cannot be deleted.</p>
                    <form id="supplier-form" class="supplier-form">
                      <label for="new-supplier">Add supplier or collector
                        <input id="new-supplier" name="name" maxlength="160" required>
                      </label>
                      <button type="submit">Add supplier</button>
                    </form>
                    <div class="supplier-tools">
                      <input id="supplier-search" type="search" placeholder="Search suppliers"
                        aria-label="Search suppliers">
                      <span id="supplier-count" class="detail"></span>
                    </div>
                    <div class="table-wrap">
                      <table>
                        <thead><tr><th>Supplier / collector</th><th>Transactions</th>
                          <th>Total weight</th><th>Total value</th><th>Last intake</th>
                          <th>Actions</th></tr></thead>
                        <tbody id="supplier-rows"></tbody>
                      </table>
                      <p id="supplier-empty" class="empty" hidden>No suppliers found.</p>
                    </div>
                    <p id="supplier-status" class="status" role="status" aria-live="polite"></p>
                  </section>
                </section>
              </main>
              <script>
                const numberFormat = new Intl.NumberFormat(undefined, { maximumFractionDigits: 3 });
                const form = document.querySelector("#intake-form");
                const statusLine = document.querySelector("#status");
                const saveButton = document.querySelector("#save-button");
                const supplierForm = document.querySelector("#supplier-form");
                const supplierStatus = document.querySelector("#supplier-status");
                let suppliers = [];
                function setStatus(message, kind) {
                  statusLine.textContent = message;
                  statusLine.className = "status" + (kind ? " " + kind : "");
                }
                function setSupplierStatus(message, kind) {
                  supplierStatus.textContent = message;
                  supplierStatus.className = "status" + (kind ? " " + kind : "");
                }
                const pageNames = ["home", "about", "services", "dashboard", "suppliers"];
                function showPage(page) {
                  const selectedPage = pageNames.includes(page) ? page : "home";
                  document.querySelectorAll(".page").forEach((section) => {
                    section.hidden = section.id !== "page-" + selectedPage;
                  });
                  document.querySelectorAll("nav [data-page-link]").forEach((link) => {
                    if (link.dataset.pageLink === selectedPage) {
                      link.setAttribute("aria-current", "page");
                    } else {
                      link.removeAttribute("aria-current");
                    }
                  });
                  if (selectedPage === "suppliers") {
                    refreshSuppliers().catch((error) =>
                      setSupplierStatus(error.message, "error"));
                  }
                }
                window.addEventListener("hashchange", () =>
                  showPage(window.location.hash.slice(1)));
                showPage(window.location.hash.slice(1) || "home");
                async function refreshSuppliers() {
                  const response = await fetch("/api/suppliers");
                  if (!response.ok) throw new Error("Supplier list could not be loaded.");
                  suppliers = await response.json();
                  document.querySelector("#home-suppliers").textContent =
                    new Intl.NumberFormat().format(suppliers.length);
                  const list = document.querySelector("#supplier-options");
                  list.replaceChildren();
                  for (const supplier of suppliers) {
                    const option = document.createElement("option");
                    option.value = supplier.name;
                    list.append(option);
                  }
                  renderSuppliers();
                }
                function renderSuppliers() {
                  const query = document.querySelector("#supplier-search").value.trim().toLocaleLowerCase();
                  const matches = suppliers.filter((supplier) =>
                    supplier.name.toLocaleLowerCase().includes(query));
                  const rows = document.querySelector("#supplier-rows");
                  rows.replaceChildren();
                  document.querySelector("#supplier-count").textContent =
                    matches.length + (matches.length === 1 ? " supplier" : " suppliers");
                  document.querySelector("#supplier-empty").hidden = matches.length !== 0;
                  for (const supplier of matches) {
                    const row = document.createElement("tr");
                    for (const value of [supplier.name, numberFormat.format(supplier.transactionCount),
                        numberFormat.format(supplier.totalWeightKg) + " kg", supplier.totalValueFormatted,
                        supplier.lastIntakeAt || "No intake yet"]) {
                      const cell = document.createElement("td");
                      cell.textContent = value;
                      row.append(cell);
                    }
                    const actions = document.createElement("td");
                    const buttons = document.createElement("div");
                    buttons.className = "action-buttons";
                    const rename = document.createElement("button");
                    rename.type = "button";
                    rename.className = "secondary";
                    rename.textContent = "Rename";
                    rename.addEventListener("click", () => renameSupplier(supplier.name));
                    buttons.append(rename);
                    if (supplier.transactionCount === 0) {
                      const remove = document.createElement("button");
                      remove.type = "button";
                      remove.className = "danger";
                      remove.textContent = "Delete";
                      remove.addEventListener("click", () => deleteSupplier(supplier.name));
                      buttons.append(remove);
                    } else {
                      const protectedLabel = document.createElement("span");
                      protectedLabel.className = "inactive";
                      protectedLabel.textContent = "History protected";
                      buttons.append(protectedLabel);
                    }
                    actions.append(buttons);
                    row.append(actions);
                    rows.append(row);
                  }
                }
                document.querySelector("#supplier-search").addEventListener("input", renderSuppliers);
                supplierForm.addEventListener("submit", async (event) => {
                  event.preventDefault();
                  if (!supplierForm.reportValidity()) return;
                  try {
                    const response = await fetch("/api/suppliers", {
                      method: "POST",
                      headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                      body: new URLSearchParams(new FormData(supplierForm))
                    });
                    const result = await response.json();
                    if (!response.ok) throw new Error(result.error || "Supplier could not be added.");
                    supplierForm.reset();
                    setSupplierStatus("Supplier added.", "success");
                    await refreshSuppliers();
                  } catch (error) {
                    setSupplierStatus(error.message || "Supplier could not be added.", "error");
                  }
                });
                async function renameSupplier(currentName) {
                  const name = window.prompt("Enter the new supplier or collector name:", currentName);
                  if (name === null) return;
                  try {
                    const response = await fetch("/api/suppliers", {
                      method: "PUT",
                      headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                      body: new URLSearchParams({currentName, name})
                    });
                    const result = await response.json();
                    if (!response.ok) throw new Error(result.error || "Supplier could not be renamed.");
                    setSupplierStatus("Supplier renamed; intake history was updated.", "success");
                    await refreshSuppliers();
                  } catch (error) {
                    setSupplierStatus(error.message || "Supplier could not be renamed.", "error");
                  }
                }
                async function deleteSupplier(name) {
                  if (!window.confirm("Delete supplier " + name + "?")) return;
                  try {
                    const response = await fetch("/api/suppliers", {
                      method: "DELETE",
                      headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                      body: new URLSearchParams({name})
                    });
                    const result = await response.json();
                    if (!response.ok) throw new Error(result.error || "Supplier could not be deleted.");
                    setSupplierStatus("Supplier deleted.", "success");
                    await refreshSuppliers();
                  } catch (error) {
                    setSupplierStatus(error.message || "Supplier could not be deleted.", "error");
                  }
                }
                async function refreshDashboard() {
                  const response = await fetch("/api/dashboard");
                  if (!response.ok) throw new Error("Dashboard could not be loaded.");
                  const data = await response.json();
                  document.querySelector("#total-weight").textContent =
                    numberFormat.format(data.summary.totalWeightKg) + " kg";
                  document.querySelector("#home-weight").textContent =
                    numberFormat.format(data.summary.totalWeightKg) + " kg";
                  document.querySelector("#transaction-count").textContent =
                    new Intl.NumberFormat().format(data.summary.transactionCount);
                  document.querySelector("#home-count").textContent =
                    new Intl.NumberFormat().format(data.summary.transactionCount);
                  document.querySelector("#total-value").textContent = data.summary.totalValueFormatted;
                  const body = document.querySelector("#transactions");
                  body.replaceChildren();
                  for (const item of data.transactions) {
                    const row = document.createElement("tr");
                    for (const value of [item.createdAt, item.supplier, item.material,
                        numberFormat.format(item.weightKg), item.rateFormatted, item.amountFormatted]) {
                      const cell = document.createElement("td");
                      cell.textContent = value;
                      row.append(cell);
                    }
                    body.append(row);
                  }
                  document.querySelector("#empty-state").hidden = data.transactions.length !== 0;
                }
                form.addEventListener("submit", async (event) => {
                  event.preventDefault();
                  if (!form.reportValidity()) return;
                  saveButton.disabled = true;
                  setStatus("Saving intake…", "");
                  try {
                    const response = await fetch("/api/intakes", {
                      method: "POST",
                      headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
                      body: new URLSearchParams(new FormData(form))
                    });
                    const result = await response.json();
                    if (!response.ok) throw new Error(result.error || "Intake could not be saved.");
                    form.elements.supplier.value = "";
                    form.elements.weightKg.value = "";
                    form.elements.ratePerKg.value = "";
                    setStatus("Intake saved.", "success");
                    await refreshDashboard();
                  } catch (error) {
                    setStatus(error.message || "Could not connect to the local app.", "error");
                  } finally {
                    saveButton.disabled = false;
                  }
                });
                refreshDashboard().catch((error) => setStatus(error.message, "error"));
                refreshSuppliers().catch((error) => setSupplierStatus(error.message, "error"));
              </script>
            </body>
            </html>
            """;

    private final RecyclingDatabase database;
    private HttpServer server;
    private ExecutorService executor;

    RecyclingHttpServer(RecyclingDatabase database) {
        this.database = database;
    }

    void start() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 0);
        executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    void stop() {
        if (server != null) {
            server.stop(1);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            addSecurityHeaders(exchange);
            try {
                if (!isLocalRequest(exchange)) {
                    sendJson(exchange, 403, "{\"error\":\"Local browser requests only.\"}");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if ("/".equals(path) && "GET".equals(method)) {
                    send(exchange, 200, "text/html; charset=utf-8", PAGE);
                } else if ("/api/dashboard".equals(path) && "GET".equals(method)) {
                    sendJson(exchange, 200, dashboardJson());
                } else if ("/api/intakes".equals(path) && "POST".equals(method)) {
                    createIntake(exchange);
                } else if ("/api/suppliers".equals(path) && "GET".equals(method)) {
                    sendJson(exchange, 200, suppliersJson());
                } else if ("/api/suppliers".equals(path) && "POST".equals(method)) {
                    addSupplier(exchange);
                } else if ("/api/suppliers".equals(path) && "PUT".equals(method)) {
                    renameSupplier(exchange);
                } else if ("/api/suppliers".equals(path) && "DELETE".equals(method)) {
                    deleteSupplier(exchange);
                } else {
                    sendJson(exchange, 404, "{\"error\":\"Not found.\"}");
                }
            } catch (IllegalArgumentException exception) {
                sendJson(exchange, 400, jsonError(exception.getMessage()));
            } catch (ArithmeticException exception) {
                sendJson(exchange, 400, "{\"error\":\"The weight or rate is too large.\"}");
            } catch (java.sql.SQLException exception) {
                int status = exception.getErrorCode() == 19 ? 409 : 500;
                String message = exception.getErrorCode() == 19
                        ? "A supplier with that name already exists."
                        : exception.getMessage();
                sendJson(exchange, status, jsonError(message));
                if (status == 500) {
                    exception.printStackTrace(System.err);
                }
            } catch (Exception exception) {
                sendJson(exchange, 500, "{\"error\":\"The local database request failed.\"}");
                exception.printStackTrace(System.err);
            }
        }
    }

    private boolean isLocalRequest(HttpExchange exchange) {
        String expectedHost = "127.0.0.1:" + PORT;
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (!expectedHost.equalsIgnoreCase(host)) {
            return false;
        }
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        return origin == null || ("http://" + expectedHost).equalsIgnoreCase(origin);
    }

    private void createIntake(HttpExchange exchange) throws IOException, java.sql.SQLException {
        if (!"application/x-www-form-urlencoded".equalsIgnoreCase(
                exchange.getRequestHeaders().getFirst("Content-Type") == null
                        ? "" : exchange.getRequestHeaders().getFirst("Content-Type").split(";", 2)[0].trim())) {
            throw new IllegalArgumentException("Submit the intake using the dashboard form.");
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("The intake form is too large.");
        }
        Map<String, String> fields = parseForm(new String(body, StandardCharsets.UTF_8));
        String supplier = fields.getOrDefault("supplier", "").trim();
        String material = fields.getOrDefault("material", "");
        if (supplier.isEmpty()) {
            throw new IllegalArgumentException("Enter the supplier or collector name.");
        }
        if (supplier.length() > 160) {
            throw new IllegalArgumentException("Supplier / collector name must be 160 characters or fewer.");
        }
        if (!MATERIALS.contains(material)) {
            throw new IllegalArgumentException("Choose a valid material type.");
        }
        BigDecimal weight = parsePositive(fields.get("weightKg"), "weight");
        BigDecimal rate = parsePositive(fields.get("ratePerKg"), "rate");
        if (weight.scale() > 3) {
            throw new IllegalArgumentException("Weight can have up to 3 decimal places.");
        }
        if (rate.scale() > 2) {
            throw new IllegalArgumentException("Rate can have up to 2 decimal places.");
        }
        weight.setScale(3, RoundingMode.HALF_UP).movePointRight(3).longValueExact();
        rate.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        long amountCents = weight.multiply(rate).movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (amountCents <= 0) {
            throw new IllegalArgumentException("The estimated value must be greater than zero.");
        }
        database.record(supplier, material, weight, rate);
        sendJson(exchange, 201, "{\"saved\":true}");
    }

    private void addSupplier(HttpExchange exchange) throws IOException, java.sql.SQLException {
        Map<String, String> fields = readForm(exchange);
        String name = validatedSupplierName(fields.get("name"));
        database.addSupplier(name);
        sendJson(exchange, 201, "{\"saved\":true}");
    }

    private void renameSupplier(HttpExchange exchange) throws IOException, java.sql.SQLException {
        Map<String, String> fields = readForm(exchange);
        String currentName = validatedSupplierName(fields.get("currentName"));
        String newName = validatedSupplierName(fields.get("name"));
        database.renameSupplier(currentName, newName);
        sendJson(exchange, 200, "{\"saved\":true}");
    }

    private void deleteSupplier(HttpExchange exchange) throws IOException, java.sql.SQLException {
        Map<String, String> fields = readForm(exchange);
        database.deleteSupplier(validatedSupplierName(fields.get("name")));
        sendJson(exchange, 200, "{\"deleted\":true}");
    }

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        if (!"application/x-www-form-urlencoded".equalsIgnoreCase(
                exchange.getRequestHeaders().getFirst("Content-Type") == null
                        ? "" : exchange.getRequestHeaders().getFirst("Content-Type").split(";", 2)[0].trim())) {
            throw new IllegalArgumentException("Submit the request using the dashboard form.");
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("The form is too large.");
        }
        return parseForm(new String(body, StandardCharsets.UTF_8));
    }

    private String validatedSupplierName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter the supplier or collector name.");
        }
        if (name.length() > 160) {
            throw new IllegalArgumentException("Supplier / collector name must be 160 characters or fewer.");
        }
        return name;
    }

    private BigDecimal parsePositive(String value, String field) {
        try {
            BigDecimal result = new BigDecimal(value == null ? "" : value.trim());
            if (result.signum() <= 0) {
                throw new IllegalArgumentException(
                        Character.toUpperCase(field.charAt(0)) + field.substring(1)
                                + " must be greater than zero.");
            }
            return result;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid number for " + field + ".");
        }
    }

    private Map<String, String> parseForm(String encoded) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (encoded.isEmpty()) {
            return fields;
        }
        for (String pair : encoded.split("&")) {
            int separator = pair.indexOf('=');
            String name = URLDecoder.decode(separator < 0 ? pair : pair.substring(0, separator),
                    StandardCharsets.UTF_8);
            String value = URLDecoder.decode(separator < 0 ? "" : pair.substring(separator + 1),
                    StandardCharsets.UTF_8);
            if (fields.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Duplicate form field: " + name);
            }
        }
        return fields;
    }

    private String dashboardJson() throws java.sql.SQLException {
        RecyclingDatabase.Summary summary = database.getSummary();
        NumberFormat money = NumberFormat.getCurrencyInstance();
        List<RecyclingDatabase.Transaction> transactions = database.getRecentTransactions();
        StringBuilder json = new StringBuilder("{\"summary\":{\"transactionCount\":")
                .append(summary.transactionCount())
                .append(",\"totalWeightKg\":").append(summary.totalWeightKg())
                .append(",\"totalValueFormatted\":").append(jsonString(
                        money.format(BigDecimal.valueOf(summary.totalValueCents(), 2))))
                .append("},\"transactions\":[");
        for (int i = 0; i < transactions.size(); i++) {
            RecyclingDatabase.Transaction transaction = transactions.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"createdAt\":").append(jsonString(transaction.createdAt()))
                    .append(",\"supplier\":").append(jsonString(transaction.supplier()))
                    .append(",\"material\":").append(jsonString(transaction.material()))
                    .append(",\"weightKg\":").append(transaction.weightKg().toPlainString())
                    .append(",\"rateFormatted\":").append(jsonString(
                            money.format(BigDecimal.valueOf(transaction.rateCents(), 2))))
                    .append(",\"amountFormatted\":").append(jsonString(
                            money.format(BigDecimal.valueOf(transaction.amountCents(), 2))))
                    .append('}');
        }
        return json.append("]}").toString();
    }

    private String suppliersJson() throws java.sql.SQLException {
        NumberFormat money = NumberFormat.getCurrencyInstance();
        StringBuilder json = new StringBuilder("[");
        List<RecyclingDatabase.SupplierSummary> suppliers = database.getSuppliers();
        for (int i = 0; i < suppliers.size(); i++) {
            RecyclingDatabase.SupplierSummary supplier = suppliers.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"name\":").append(jsonString(supplier.name()))
                    .append(",\"transactionCount\":").append(supplier.transactionCount())
                    .append(",\"totalWeightKg\":").append(supplier.totalWeightKg())
                    .append(",\"totalValueFormatted\":").append(jsonString(
                            money.format(BigDecimal.valueOf(supplier.totalValueCents(), 2))))
                    .append(",\"lastIntakeAt\":").append(jsonString(
                            supplier.lastIntakeAt() == null ? "" : supplier.lastIntakeAt()))
                    .append('}');
        }
        return json.append(']').toString();
    }

    private String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private String jsonError(String message) {
        return "{\"error\":" + jsonString(message == null ? "Invalid request." : message) + "}";
    }

    private void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; "
                        + "connect-src 'self'; img-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", body);
    }

    private void send(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
