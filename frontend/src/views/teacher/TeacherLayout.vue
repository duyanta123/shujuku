<template>
  <AppLayout :config="teacherLayoutConfig">
    <router-view />
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import { teacherLayoutConfig } from '@/config/layoutConfig'
</script>
