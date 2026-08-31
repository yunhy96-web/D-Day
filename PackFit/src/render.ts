import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import type { Box, PackResult } from './types';

// 모델 좌표(x=가로, y=세로, z=높이) → Three(x, y=up, z). 원점 = 상자 내벽 좌-앞-바닥 모서리.
export function createRenderer(el: HTMLElement) {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0xf5f5f5);
  const camera = new THREE.PerspectiveCamera(45, 1, 1, 100000);
  const renderer = new THREE.WebGLRenderer({ antialias: true });
  el.appendChild(renderer.domElement);
  const controls = new OrbitControls(camera, renderer.domElement);
  scene.add(new THREE.AmbientLight(0xffffff, 1.2));
  const sun = new THREE.DirectionalLight(0xffffff, 1.5);
  sun.position.set(1, 2, 1.5);
  scene.add(sun);
  const group = new THREE.Group();
  scene.add(group);
  let items: THREE.Object3D[][] = []; // placed 순서대로 [mesh, edge]

  // hover 툴팁
  const tip = el.querySelector<HTMLElement>('#tip')!;
  const ray = new THREE.Raycaster();
  renderer.domElement.addEventListener('pointermove', e => {
    const r = renderer.domElement.getBoundingClientRect();
    ray.setFromCamera(new THREE.Vector2(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1), camera);
    const hit = ray.intersectObjects(items.map(o => o[0]).filter(o => o.visible))[0];
    tip.hidden = !hit;
    if (!hit) return;
    tip.textContent = hit.object.userData.label;
    tip.style.left = `${e.clientX - r.left + 12}px`;
    tip.style.top = `${e.clientY - r.top + 12}px`;
  });
  renderer.domElement.addEventListener('pointerleave', () => (tip.hidden = true));

  const resize = () => {
    const { clientWidth: w, clientHeight: h } = el;
    renderer.setSize(w, h);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  };
  new ResizeObserver(resize).observe(el);
  resize();
  renderer.setAnimationLoop(() => { controls.update(); renderer.render(scene, camera); });

  const setStep = (n: number) => items.forEach((o, i) => o.forEach(m => (m.visible = i < n)));

  const draw = (box: Box, result: PackResult, colorOf: (id: string) => string) => {
    group.clear();
    items = [];
    const inner = { w: box.w - 2 * box.wall, d: box.d - 2 * box.wall, h: box.h - 2 * box.wall };

    // 상자 외곽(실선) + 유효 공간(점선)
    const outer = new THREE.BoxGeometry(box.w, box.h, box.d).translate(box.w / 2 - box.wall, box.h / 2 - box.wall, box.d / 2 - box.wall);
    group.add(new THREE.LineSegments(new THREE.EdgesGeometry(outer), new THREE.LineBasicMaterial({ color: 0x333333 })));
    const eff = new THREE.BoxGeometry(inner.w, inner.h, inner.d).translate(inner.w / 2, inner.h / 2, inner.d / 2);
    const dashed = new THREE.LineSegments(new THREE.EdgesGeometry(eff), new THREE.LineDashedMaterial({ color: 0x999999, dashSize: 8, gapSize: 6 }));
    dashed.computeLineDistances();
    group.add(dashed);
    // 바닥면
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(box.w, box.d), new THREE.MeshBasicMaterial({ color: 0xe8e0d0, side: THREE.DoubleSide }));
    floor.rotation.x = -Math.PI / 2;
    floor.position.set(box.w / 2 - box.wall, -box.wall - 0.5, box.d / 2 - box.wall);
    group.add(floor);

    for (const p of result.placed) {
      const geo = new THREE.BoxGeometry(p.dim.w, p.dim.h, p.dim.d);
      const mesh = new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ color: colorOf(p.item.id) }));
      mesh.position.set(p.pos.x + p.dim.w / 2, p.pos.z + p.dim.h / 2, p.pos.y + p.dim.d / 2);
      mesh.userData.label = `${p.item.name} ${p.dim.w}×${p.dim.d}×${p.dim.h} @ (${p.pos.x}, ${p.pos.y}, ${p.pos.z})`;
      group.add(mesh);
      const edge = new THREE.LineSegments(new THREE.EdgesGeometry(geo), new THREE.LineBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.35 }));
      edge.position.copy(mesh.position);
      group.add(edge);
      items.push([mesh, edge]);
    }

    const size = Math.max(box.w, box.d, box.h);
    controls.target.set(box.w / 2 - box.wall, box.h / 2 - box.wall, box.d / 2 - box.wall);
    camera.position.set(box.w / 2 + size * 1.2, box.h + size * 0.9, box.d / 2 + size * 1.4);
  };
  return { draw, setStep };
}
