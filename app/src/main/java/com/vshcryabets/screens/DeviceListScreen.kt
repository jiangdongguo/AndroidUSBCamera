/*
 * Copyright 2024 vschryabets@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vshcryabets.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DeviceListScreen {
    @Composable
    fun ProductItem(product: UsbDevice, onItemClick: (UsbDevice) -> Unit) {
        Column(modifier = Modifier
            .clickable { onItemClick(product) }
            .padding(8.dp)) {
            Text(
                text = "Vendor: ${product.vendorName}",
                fontSize = 18.sp
            )

            Text(
                text = "ID: ${product.displayName}",
                fontSize = 14.sp
            )

            Text(
                text = "Classes: ${product.classesStr}",
                fontSize = 14.sp
            )

        }
    }

    @Composable
    fun ScreenContent(viewModel: DeviceListViewModel) {
        val state by viewModel.state.collectAsState(DeviceListViewState())
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    viewModel.onEnumarate()
                },

                ) {
                Text("Reload USB devices")
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                this.items(state.devices) {
                    ProductItem(product = it) {
                        viewModel.onClick(it)
                    }
                }
            }
        }
    }
}