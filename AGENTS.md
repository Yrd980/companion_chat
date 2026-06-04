## Design Principles

### Local-first privacy

Anime Companion is designed around private, on-device interaction. Prefer local
models, local storage, and explicit user configuration before adding any remote
dependency.

### Companion before assistant

The product should feel like a durable companion, not a generic task bot.
Conversation design should preserve emotional continuity, role identity, memory,
and user preference context without making the interaction feel mechanical.

### User-controlled boundaries

Interaction boundaries should stay close to the user. Make model choice,
backend choice, voice behavior, role behavior, and cloud endpoints visible and
configurable instead of hiding them behind automatic remote policy.

### Graceful acceleration

Use device acceleration when it works, but keep the app usable when it does not.
GPU and NPU paths should report clear diagnostics and fall back to the best
available local backend.

### Durable memory

Memories and preferences should be stored, retrieved, and injected in ways that
support long-term relationship continuity. Treat memory as user-owned context:
accurate, editable, and easy to inspect.

### Voice-first, text-capable

The primary interaction loop should work well by voice, while text input, image
input, and manual replay remain reliable secondary controls.

### Explicit model packages

Large model files are not bundled into the repository. Runtime features should
validate required model packages, explain missing files clearly, and avoid
crashing when optional local models are unavailable.

### Simple surfaces

UI should favor quiet, repeatable product workflows over marketing surfaces.
Screens should make current state, available controls, and failure messages easy
to scan.

### Small, traceable changes

Implementation changes should stay scoped to the requested behavior. Prefer
existing repositories, engines, ViewModels, and Compose patterns before adding
new abstractions.
