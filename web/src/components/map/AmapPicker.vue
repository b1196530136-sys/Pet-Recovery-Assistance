<template>
  <div class="amap-picker">
    <div style="position: relative; margin-bottom: 8px;">
      <el-input
        v-model="searchText"
        placeholder="搜索地址，如：北京市朝阳区"
        clearable
        @input="onSearchInput"
      />
      <div v-if="searchResults.length" class="search-results">
        <div
          v-for="item in searchResults"
          :key="item.id"
          class="search-item"
          @click="selectSearchResult(item)"
        >
          {{ item.name }}
          <span style="color: #909399; font-size: 12px;">{{ item.address || item.district }}</span>
        </div>
      </div>
    </div>
    <div ref="mapContainer" class="amap-map"></div>
    <div v-if="address" class="selected-address">
      已选位置: {{ address }} ({{ longitude }}, {{ latitude }})
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:modelValue'])

const mapContainer = ref(null)
const longitude = ref(props.modelValue.lng || '')
const latitude = ref(props.modelValue.lat || '')
const address = ref(props.modelValue.address || '')
const searchText = ref(props.modelValue.address || '')
const searchResults = ref([])

let map = null
let marker = null
let AMap = null
let searchTimer = null

onMounted(() => {
  window._AMapSecurityConfig = {
    securityJsCode: 'd2a114ab986fe29825688ec540030b5a',
  }

  AMapLoader.load({
    key: 'eb4473d0bf626ceed61dbb79c86ba988',
    version: '2.0',
    plugins: ['AMap.Geocoder', 'AMap.PlaceSearch', 'AMap.AutoComplete'],
  }).then((AMapInstance) => {
    AMap = AMapInstance

    const center = props.modelValue.lng && props.modelValue.lat
      ? [parseFloat(props.modelValue.lng), parseFloat(props.modelValue.lat)]
      : [116.397428, 39.90923]

    map = new AMap.Map(mapContainer.value, {
      zoom: 15,
      center,
    })

    if (props.modelValue.lng && props.modelValue.lat) {
      marker = new AMap.Marker({ position: center })
      map.add(marker)
    }

    map.on('click', (e) => {
      const lng = e.lnglat.getLng().toFixed(7)
      const lat = e.lnglat.getLat().toFixed(7)
      setMarker(lng, lat)

      const geocoder = new AMap.Geocoder()
      geocoder.getAddress([parseFloat(lng), parseFloat(lat)], (status, result) => {
        if (status === 'complete' && result.info === 'OK') {
          address.value = result.regeocode.formattedAddress
          searchText.value = result.regeocode.formattedAddress
          emit('update:modelValue', { lng: longitude.value, lat: latitude.value, address: address.value })
        }
      })
    })
  })
})

function setMarker(lng, lat) {
  longitude.value = lng
  latitude.value = lat
  if (marker) map.remove(marker)
  marker = new AMap.Marker({ position: [parseFloat(lng), parseFloat(lat)] })
  map.add(marker)
  map.setCenter([parseFloat(lng), parseFloat(lat)])
}

function onSearchInput() {
  clearTimeout(searchTimer)
  searchResults.value = []
  if (!searchText.value.trim() || !AMap) return

  searchTimer = setTimeout(() => {
    const placeSearch = new AMap.PlaceSearch({
      pageSize: 6,
      pageIndex: 1,
    })
    placeSearch.search(searchText.value, (status, result) => {
      if (status === 'complete' && result.poiList) {
        searchResults.value = result.poiList.pois || []
      }
    })
  }, 300)
}

function selectSearchResult(item) {
  searchResults.value = []
  const lng = item.location.getLng().toFixed(7)
  const lat = item.location.getLat().toFixed(7)
  setMarker(lng, lat)
  address.value = item.address || item.name
  searchText.value = item.name
  emit('update:modelValue', { lng, lat, address: address.value })
}
</script>

<style scoped>
.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  z-index: 999;
  max-height: 240px;
  overflow-y: auto;
}
.search-item {
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  border-bottom: 1px solid #f0f0f0;
}
.search-item:hover {
  background: #ecf5ff;
}
.search-item span {
  display: block;
  margin-top: 2px;
}
.amap-map {
  width: 100%;
  height: min(400px, 48vh);
  border: 1px solid #edf1f5;
  border-radius: 8px;
  overflow: hidden;
}
.selected-address {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .amap-map {
    height: 340px;
  }
}
</style>
