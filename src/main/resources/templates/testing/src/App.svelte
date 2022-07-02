<script lang="ts">
	import Button from "./button/Button.svelte"

	export let name;
</script>

<main>
	<Button />
	<h1>Hello {name}!</h1>
	<p>Visit the <a href="https://svelte.dev/tutorial">Svelte tutorial</a> to learn how to build Svelte apps.</p>
</main>

<style>
	
</style>