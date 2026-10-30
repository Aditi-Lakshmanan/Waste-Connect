# WasteConnect UI Audit

Audit date: 2026-10-29

Status describes the current implementation before the fixes listed below. `Broken` includes a requested feature that is absent or a control whose displayed behavior is misleading.

| Screen | Control | Expected behaviour | Current status | Fix planned |
|---|---|---|---|---|
| Login | Account type selector | Select Admin, Household/Citizen, Collector, or Waste Picker | Working | Keep all roles and validate selected role at authentication |
| Login | Email/password fields and Login | Authenticate against stored users and open matching workspace | Working | Keep service-backed authentication |
| Login | Demo account hint | Show credentials for selected seeded role | Working | Keep synchronized with selected role |
| Login | Registration/password reset | Allow eligible users to create or recover accounts | Broken | Add only if account recovery/registration policy and service support are defined |
| Admin Overview | Overview navigation | Show current database-backed municipal counts | Working | Keep service-backed; make counts refresh on relevant data changes |
| Admin modules | Sidebar navigation | Open each admin module | Working | Keep navigation and remove any module control without a real operation |
| Admin modules | Search field/Search button | Search current module records | Working | Implement repository/service-side filtering and consistent paging across modules |
| Admin modules | Refresh button | Reload the current module | Working | Retain as a manual fallback; event-driven refresh is still needed |
| Admin modules | Previous/Next pagination | Navigate the current module's pages | Broken | Add per-module pagination or hide pagination where it is unsupported |
| Admin modules | Data tables | Show readable, current service-backed records | Working | Keep stable column sizing; add helpful empty states and data refresh subscriptions |
| Admin Households | Household table and CRUD actions | Search, page, create, edit, delete, and store household coordinates | Broken | Service-backed CRUD and persistence tests are added; manual JavaFX dialog click-through remains |
| Admin Zones | Zones table and CRUD actions | View, create, edit, and delete municipal zones | Broken | UI dialogs are added; verify them through a JavaFX UI test/manual click-through; backend CRUD persistence is tested |
| Admin Smart Bins | Filtered paginated table and CRUD actions | Search/filter, page, create, edit, and delete bins | Broken | UI actions and service persistence are added/tested; manual dialog click-through and live simulator refresh remain |
| Admin Vehicles | Vehicles table and CRUD actions | View, create, edit, and delete fleet vehicles | Broken | UI dialogs are added; verify them through JavaFX click-through; backend CRUD persistence is tested |
| Admin Collectors | Collector table and CRUD actions | Create authenticated collector accounts, edit assignments, deactivate, delete | Broken | UI actions and account persistence/hash behavior are tested; manual click-through remains |
| Admin Complaints | Complaints table and zone-collector assignment | Assign, track, resolve, and escalate complaints | Broken | Assignment action and service persistence are tested; Kanban/status actions and nearest-distance assignment remain |
| Admin Waste Pickers | Waste picker table and approve/reject actions | Review and approve/reject picker applications | Broken | Actions and service persistence are added; JavaFX click-through remains unverified |
| Admin Analytics | Analytics table | Show calculated municipal performance measures from persisted records | Working | KPIs now derive from route stops, logs, complaints, and bin waste types; savings baseline and charts remain |
| Admin Live Map | Map, zone/date selectors, pending/collected lists, route controls | Display households, bins, status, routes, and support dispatch actions | Broken | Implement from persisted coordinates, collection state, and route data |
| Admin Hotspots | Ranked hotspot list/map markers | Show complaint/overflow-derived hotspots and recommendations | Broken | Add service-backed view and map overlay |
| Admin Benchmark | Run benchmark and results view | Compare Fixed/Priority routes and persist/review results | Broken | Add asynchronous benchmark UI and persisted results |
| Admin Simulator | Start/stop/speed/surge/reset controls | Simulate bin fill changes and restore demo state | Broken | Controls are wired to the simulator; verify async persistence/live table refresh, stop lifecycle, and reset semantics |
| Admin Reports | CSV export controls | Export households, collections, complaints, and points ledger | Broken | Add file chooser and service-backed exports |
| Admin Settings | Priority/SLA/points/threshold controls | Persist settings and apply changes immediately | Broken | Add validated settings form and change notifications |
| Household Overview | Next pickup and points labels | Show upcoming request and current stored points balance | Working | Add live event refresh and richer collection status/ETA |
| Household Collections | Type/weight/date/address fields and Request button | Persist a pickup request and show request history | Working | Add cancel/status lifecycle and dispatch integration |
| Household Points | Amount/reward fields and Redeem button | Atomically deduct points and persist ledger entry | Working | Generate redemption code; add charts/rank and event-driven refresh |
| Household Complaints | Category/description/location and Submit button | Persist complaint, automatically assign it, and show status/timeline | Working | Zone-based assignment and collector visibility are tested; nearest-distance assignment and citizen timeline remain |
| Household Recyclables | Material/weight/address/location and Offer button | Persist recyclable listing and show picker claim/completion state | Working | Show accepting picker and award citizen points on completion |
| Household Notifications | Notification tab, unread count, mark selected/all read | View and persist read state for notifications | Working | Add click-through navigation when target screens/routes are defined |
| Household Segregation Guide | Search field and guide table | Search disposal instructions by material/category | Working | Move guide catalog to managed database content if administrators must edit it |
| Household Complaint Timeline | View selected complaint timeline | Show persisted status transitions and notes | Working | Add automatic SLA status refresh via events |
| Household Leaderboard | Ranked table | Show service-backed household rankings | Working | Add zone selector and citizen's own rank highlight |
| Household Points | Redeem action | Atomically deduct points and show a unique redemption code | Working | Add voucher/tax-rebate catalog and code validation/redeem workflow |
| Collector Route | Route table, quality/weight/citizen fields and Record button | Record a route stop, segregation, and reward effects transactionally | Working | Add checkbox workflow, per-waste weights, undo, progress, ETA, and day summary |
| Collector Route | Obstruction field and Report button | Replan remaining route and update route view | Working | Publish RouteReplanned event and verify route-stop persistence |
| Collector Complaints | Assigned complaint table, resolution notes, Resolve button | Resolve assigned complaint and notify citizen | Working | Add Start/In Progress action, status timeline refresh, and SLA data |
| Collector Route | Route assignment/empty state | Clearly show today's assignment state | Broken | Add useful empty-state text and admin dispatch integration |
| Waste Picker Overview | Verification and earnings labels | Show stored verification and earnings | Working | Refresh through events and add historical earnings detail |
| Waste Picker Available Pickups | Location fields, listings table, and Claim button | Sort nearby offers by entered coordinates and atomically claim one | Working | Listing lookup sorts by distance; database conditional claim prevents duplicate acceptance; manual UI click-through remains |
| Waste Picker My Pickups | Weight field and Complete button | Complete own claimed listing, update earnings, and credit citizen points | Working | Service transaction and regression test cover listing state, earnings, points, and ledger; add earnings history/monthly chart |
| All role workspaces | Refresh data button | Manually reload current role data | Working | Keep as fallback; event-driven refresh is primary for wired domain events |
| All role workspaces | Logout button | Clear session and return to login | Broken | Call AuthService.logout before showing LoginView |
| Legacy DashboardView | Status/role/email cards and overview list | Present role operations | Broken | Remove or route all roles through their operational workspace |
| Application | Cross-module EventBus | Publish typed domain events and refresh subscribers; unsubscribe on close | Broken | Add singleton typed event bus, lifecycle-safe subscriptions, and service publication |
| Application | Background data loading | Load screen data off the JavaFX thread | Broken | Move repository/service reads to worker tasks and apply results on Platform.runLater |
| Application | Action feedback | Show consistent success/error feedback for every action | Working | Replace status-only feedback with consistent toast/dialog presentation where helpful |
| Database/demo data | First-launch seeded data and reset | Populate realistic connected records and restore baseline data | Working | Verify counts/relationships and add an explicit, safe reset workflow |

## Verification Notes

- The JavaFX screens compile and the existing Maven test suite passes (27 tests at the time of this audit).
- The household, collector, and picker workflows have service-backed actions, but they do not yet meet all requested lifecycle, live-update, and reporting requirements.
- The admin workspace is primarily read-only. Requested map, CRUD, kanban/SLA, benchmark, simulator, report, and settings screens are not present yet.
- No application-wide typed EventBus or UI subscription lifecycle exists.
- Do not mark a feature `Working` solely because a service method exists; verify the UI action, persistence, refresh path, and relevant test or run-through.

## Follow-up Progress

The audit table records the initial state before implementation. The following implementation slices have since been added; verification level is noted for each:

- Admin route planning now selects a zone, vehicle, collector, and Fixed/Priority strategy, persists and assigns the route, and displays route stops from the service layer. Route search and paging are wired.
- Admin complaint rows now have an assignment action that assigns to a collector in the complaint's zone and sends a notification. Collector geographic coordinates are not modeled, so this is not a true nearest-distance assignment.
- Newly raised household complaints now auto-assign to a collector in the same zone and publish/notify the assignment; a test verifies collector queue visibility. It does not choose by distance because collector coordinates are absent.
- Admin waste picker rows now have approve/reject actions using the verification service and its notification path.
- Admin Zones and Vehicles now have create/edit/delete dialogs backed by `AdminService`; service-level persistence tests pass. JavaFX dialog click-through is not yet verified, so these rows remain `Broken` in the audit until UI interaction is verified.
- Admin Households now has create/edit/delete dialogs and service validation for zone, address, and stored coordinates. A regression test covers create/update/delete and persisted coordinates; manual JavaFX dialog interaction remains unverified.
- Admin Smart Bins now has zone/type filters, database-backed search/count/page, and create/edit/delete dialogs; service tests verify CRUD and filtered paging.
- Admin Collectors now has create/edit/delete controls. Creation validates account fields/assignments and stores a salted password hash; the service regression test verifies account persistence, assignments, deactivation, and deletion.
- Collector route table now has an explicit per-stop checkbox that invokes the transactional collection service; the table also provides a specific no-route empty state.
- The Simulator module now exposes Start, Stop, speed, Festival Surge, and confirmed Reset controls against the existing engine; its user-interface persistence and live refresh still need a manual/runtime test.
- Analytics no longer fabricates waste splits or collection efficiency. It derives waste by bin type, segregation rate, resolved-complaint rate, and completed-stop efficiency from persisted records. Savings are zero until an actual baseline exists. A regression test compares returned KPIs to database aggregates.
- Admin complaint assignment and picker verification controls are wired to services; assignment and verification persistence tests pass. True nearest-distance assignment and manual UI click-through are not verified.
- Logout now clears `AuthService` session state. Admin module pagination now applies to the displayed module instead of using stale household counts.
- A singleton typed EventBus, service publications, lifecycle-bound role/admin subscriptions, and simulator overflow transition events have been added. `EventBusTest` verifies typed delivery and unsubscribe behavior.
- Route dispatch regression test verifies that a generated route is stored and available through the collector's today-route service query.
- Daily route lookup now orders by persisted `created_at` instead of random route IDs; backward-compatible schema migration and the formerly failing dispatch regression test pass.
- EventBus unit tests verify typed event delivery and unsubscribe behavior; they do not prove every cross-role screen refresh scenario.
- Picker completion now conditionally updates only a listing still claimed by that picker, and transactionally updates listing status, picker earnings, household points, and reward ledger. The integration test verifies points and ledger credit and rejects repeat completion.
- Picker available listings now use entered coordinates to sort by distance through the service layer.
- Citizen workspaces now include searchable segregation guidance, persisted complaint timelines, and a database-backed leaderboard. Redemption now creates a unique code in the points ledger and notification.
- Admin and operational role workspaces now expose persisted notifications, unread counts, and mark-selected/mark-all-read actions. Notification events publish after DB save, and persistence/read-state behavior has a regression test.
- JavaFX startup CSS warnings from the login panel gradient were removed by using a solid dark-green panel; a subsequent launch no longer emitted the Modena paint cast warning.
- Smart Bin admin now uses paginated database filters and CRUD dialogs. Its service test verifies filtered counts, page results, persisted edits, and deletion.
- Collector admin now creates accounts with salted password hashes and supports assignment edits, deactivation, and deletion. The persistence test verifies the account lifecycle.
- Admin analytics no longer invents waste category percentages or a fixed collection-efficiency value. The regression test compares its KPIs to persisted routes, segregation logs, and complaints. Distance/fuel savings remain zero until a measured baseline exists.
- New entry forms no longer prefill sample weights or coordinates. Picker distance sorting requires valid coordinates; blank input prompts for location and invalid input is not silently treated as a successful nearby sort.

The requested application is **not yet fully implemented**. Remaining work includes the live collection map and household collection status model; a true visual Kanban and nearest-distance assignment; chart-based analytics and persisted benchmarks; verified simulator persistence/reset behavior; CSV reports; settings UI and `SettingsChanged` publication; background service reads; full cross-role end-to-end refresh; collector undo, waste-by-type capture, and end-of-day summary; citizen score history and voucher/tax-rebate catalog; picker earnings history/monthly chart; and end-to-end verification for S1-S8. Some admin dialogs and simulator controls still need manual JavaFX click-through, which this environment has not performed.

The role/admin screens currently refresh event-driven data on the JavaFX thread. This is functional for the current seed size but does not yet satisfy the requested background-loading rule. The app was launched and an admin login was observed, but every control for every role has not been manually clicked through.