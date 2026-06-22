<template>
  <AppLayout :config="adminLayoutConfig">
    <router-view />
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import { adminLayoutConfig } from '@/config/layoutConfig'
</script>
