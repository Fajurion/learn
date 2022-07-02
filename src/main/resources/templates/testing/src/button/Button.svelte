<script lang="ts">
    let count = 0;
    let show = false

    function addOne() {
        count += 1;
        show = !show
    }
</script>

<main>
    <button on:click={addOne}>Clicked {count} times.</button>
    {#if show}
        <p>Shown</p>
    {:else}
        <p>Not shown</p>
    {/if}
</main>

<style>

</style>