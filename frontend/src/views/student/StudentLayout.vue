<template>
  <AppLayout :config="studentLayoutConfig">
    <router-view />
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import { studentLayoutConfig } from '@/config/layoutConfig'
</script>
