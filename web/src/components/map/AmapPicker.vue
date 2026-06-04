<template>
  <div>
    <div ref="mapContainer" style="width: 100%; height: 400px"></div>
    <div v-if="address" style="margin-top: 8px; font-size: 13px; color: #606266;">
      已选位置: {{ address }} ({{ longitude }}, {{ latitude }})
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue'])

const mapContainer = ref(null)
const longitude = ref(props.modelValue.lng || '')
const latitude = ref(props.modelValue.lat || '')
const address = ref(props.modelValue.address || '')

let map = null
let marker = null

onMounted(() => {
  window._AMapSecurityConfig = {
    securityJsCode: 'd2a114ab986fe29825688ec540030b5a',
  }

  AMapLoader.load({
    key: 'eb4473d0bf626ceed61dbb79c86ba988',
    version: '2.0',
    plugins: ['AMap.Geocoder'],
  }).then((AMap) => {
    const center = props.modelValue.lng && props.modelValue.lat
      ? [parseFloat(props.modelValue.lng), parseFloat(props.modelValue.lat)]
      : [116.397428, 39.90923]

    map = new AMap.Map(mapContainer.value, {
      zoom: 15,
      center,
    })

    if (props.modelValue.lng && props.modelValue.lat) {
      marker = new AMap.Marker({
        position: center,
      })
      map.add(marker)
    }

    map.on('click', (e) => {
      const lng = e.lnglat.getLng().toFixed(7)
      const lat = e.lnglat.getLat().toFixed(7)
      longitude.value = lng
      latitude.value = lat

      if (marker) map.remove(marker)
      marker = new AMap.Marker({
        position: [parseFloat(lng), parseFloat(lat)],
      })
      map.add(marker)

      const geocoder = new AMap.Geocoder()
      geocoder.getAddress([parseFloat(lng), parseFloat(lat)], (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
          address.value = result.regeocode.formattedAddress
          emit('update:modelValue', { lng: longitude.value, lat: latitude.value, address: address.value })
        }
      })
    })
  })
})
</script>
