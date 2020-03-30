/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 * @flow
 */

'use strict';

import React from 'react';
import {Text, StyleSheet, View, Image} from 'react-native';
import Slider, { ANDROID_DEFAULT_COLOR }  from '@react-native-community/slider';

import type {Element} from 'react';
class SliderExample extends React.Component<$FlowFixMeProps, $FlowFixMeState> {
  static defaultProps = {
    value: 0,
  };

  state = {
    value: this.props.value,
  };

  render() {
    return (
      <View>
        <Text style={styles.text}>
          {this.state.value && +this.state.value.toFixed(3)}
        </Text>
        <Slider
          {...this.props}
          onValueChange={value => this.setState({value: value})}
        />
      </View>
    );
  }
}

class SlidingStartExample extends React.Component<
  $FlowFixMeProps,
  $FlowFixMeState,
> {
  state = {
    slideStartingValue: 0,
    slideStartingCount: 0,
  };

  render() {
    return (
      <View>
        <SliderExample
          {...this.props}
          onSlidingStart={value =>
            this.setState({
              slideStartingValue: value,
              slideStartingCount: this.state.slideStartingCount + 1,
            })
          }
        />
        <Text>
          Starts: {this.state.slideStartingCount} Value:{' '}
          {this.state.slideStartingValue}
        </Text>
      </View>
    );
  }
}

class SlidingCompleteExample extends React.Component<
  $FlowFixMeProps,
  $FlowFixMeState,
> {
  state = {
    slideCompletionValue: 0,
    slideCompletionCount: 0,
  };

  render() {
    return (
      <View>
        <SliderExample
          {...this.props}
          onSlidingComplete={value =>
            this.setState({
              slideCompletionValue: value,
              slideCompletionCount: this.state.slideCompletionCount + 1,
            })
          }
        />
        <Text>
          Completions: {this.state.slideCompletionCount} Value:{' '}
          {this.state.slideCompletionValue}
        </Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  text: {
    fontSize: 14,
    textAlign: 'center',
    fontWeight: '500',
    margin: 10,
  },
});

exports.title = '<Slider>';
exports.displayName = 'SliderExample';
exports.description = 'Slider input for numeric values';
exports.examples = [
  {
    title: 'Default settings',
    render(): Element<any> {
      return <SliderExample />;
    },
  },
  {
    title: 'Initial value: 0.5',
    render(): Element<any> {
      return <SliderExample value={0.5} />;
    },
  },
  {
    title: 'minimumValue: -1, maximumValue: 2',
    render(): Element<any> {
      return <SliderExample minimumValue={-1} maximumValue={2} />;
    },
  },
  {
    title: 'step: 0.25',
    render(): Element<any> {
      return <SliderExample step={0.25} />;
    },
  },
  {
    title: 'onSlidingStart',
    render(): Element<any> {
      return <SlidingStartExample />;
    },
  },
  {
    title: 'onSlidingComplete',
    render(): Element<any> {
      return <SlidingCompleteExample />;
    },
  },
  {
    title: 'Custom min/max track tint color',
    render(): Element<any> {
      return (
        <SliderExample
          minimumTrackTintColor={'blue'}
          maximumTrackTintColor={'red'}
          value={0.5}
        />
      );
    },
  },
  {
    title: 'Custom thumb tint color',
    render(): Element<any> {
      return <SliderExample thumbTintColor={'blue'} />;
    },
  },
  {
    title: 'Custom thumb image',
    render(): Element<any> {
      return <SliderExample thumbImage={require('./uie_thumb_big.png')} />;
    },
  },
  {
    title: 'Custom track image',
    platform: 'ios',
    render(): Element<any> {
      return <SliderExample trackImage={require('./slider.png')} />;
    },
  },
  {
    title: 'Custom min/max track image',
    platform: 'ios',
    render(): Element<any> {
      return (
        <SliderExample
          minimumTrackImage={require('./slider-left.png')}
          maximumTrackImage={require('./slider-right.png')}
        />
      );
    },
  },
  {
    title: 'Inverted slider direction',
    render(): React.Element<any> {
      return <SliderExample
        value={0.6}
        inverted
        minimumTrack={() => <View style={{ flex: 1, opacity: 1 }} collapsable={false}>
          <View style={{ backgroundColor: ANDROID_DEFAULT_COLOR, flex: 1 }} />
        </View>}
      />;
    },
  },
  {
    title: 'Custom View',
    platform: 'android',
    render(): React.Element<any> {
      return (
        <SliderExample
          value={0.6}
          inverted
          thumbTintColor={'yellow'}
          minimumValue={-1}
          maximumValue={2}
          style={{width:300}}
          //minimumTrackTintColor={'blue'}
          //maximumTrackTintColor={'red'}
          thumb={<View style={{ alignItems: 'center', justifyContent: 'center', width: 40, height: 40 }} collapsable={false}>
            <View style={{ backgroundColor: 'blue', borderRadius: 50, alignItems: 'center', justifyContent: 'center', width: 30, height: 30 }} collapsable={false}>
              <Image
                source={require('./uie_thumb_big.png')}
                style={{ width: 25, height: 25 }}
              />
            </View>
          </View>}
          maximumTrack={() => <View style={{flex:1,opacity:0.3}} collapsable={false}>
            <View style={{ backgroundColor: 'blue', flex: 1 }} />
          </View>}
          minimumTrack={() => <View style={{ flex: 1, flexDirection:'row'}} collapsable={false}>
            <View style={{ backgroundColor: 'yellow', flex: 1}} />
            <View style={{ backgroundColor: 'orange', flex: 1 }} />
            <View style={{ backgroundColor: 'red', flex: 1 }} />
            <View style={{ backgroundColor: 'magenta', flex: 1 }} />
          </View>}
        />
      );
    },
  },
];